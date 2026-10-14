-- 真实访客 IP 解析核心（纯函数，无 ngx 依赖，可离线单测）
--
-- 目标：在任意 CDN / 反向代理 / 多级代理组合下，从 XFF 链或 CDN 注入的
-- "真实客户端头"中解析出访客 IP，并避免把"服务器自身 / 可信代理 / 私有地址"
-- 当成访客（历史上 XFF 直接取最右端导致 CDN 回环场景把服务器自身公网 IP
-- 解析成访客，进而被自动拉黑导致全站 403、"IP 访问 TOP1" 被污染）。
--
-- 解析语义（从右向左的可信链递归，等价于 Nginx real_ip_recursive 的常用做法，
-- 但不依赖 set_real_ip_from 的"对端必须在可信表内"前置条件，兼容存量部署）：
--   1. REAL_IP_HEADER 配置为厂商头（如 EdgeOne 的 EO-Connecting-IP）时优先采用；
--   2. 否则从 X-Forwarded-For 链最右端开始向左扫描，跳过 无效 / 私有 / 自身 / 可信段
--      条目，第一个"可信之外"的 IP 即访客；
--   3. 整条链都不可用（为空 / 全被跳过）时回退 TCP 对端（$remote_addr）。
--
-- 配置来源（均可留空，留空时行为与旧版"取最右端"在常规 CDN 直连场景一致）：
--   SELF_PUBLIC_IPS     服务器自身公网 IP，逗号分隔（手动生效，优先级最高）
--   SELF_IP_FILE        运行时文件路径，entrypoint 探测结果写入于此（默认 /app/runtime/self_ips）
--   TRUSTED_PROXY_CIDRS 可信代理 / CDN 回源网段，逗号分隔（用于跳过 CDN 回源 IP 等中间地址）
--   REAL_IP_HEADER      CDN 注入的真实客户端头名（默认 X-Forwarded-For，即只用 XFF 链）

local ip_cidr = require "ip_cidr"

local _M = {}

_M.DEFAULT_SELF_IP_FILE = "/app/runtime/self_ips"

-- 运行时文件读取（entrypoint 探测结果；文件不存在 / 不可读时返回空串）
function _M.read_self_ip_file(path)
    if not path or path == "" then return "" end
    local f = io.open(path, "r")
    if not f then return "" end
    local content = f:read("*a") or ""
    f:close()
    return content
end

-- 构建解析配置：
--   conf.self_set       自身公网 IP 集合（精确匹配）
--   conf.trusted_cidrs  可信代理 / CDN 回源网段列表
--   conf.real_ip_header 厂商真实客户端头名（默认 X-Forwarded-For）
function _M.build_conf(self_ips_raw, trusted_cidrs_raw, real_ip_header)
    local self_ips = ip_cidr.split_list(self_ips_raw)
    local self_set = {}
    for _, ip in ipairs(self_ips) do
        local normalized = ip_cidr.normalize_ip(ip)
        if normalized then
            self_set[normalized] = true
        end
    end

    local header = real_ip_header
    if not header or header == "" then
        header = "X-Forwarded-For"
    end

    return {
        self_ips = self_ips,
        self_set = self_set,
        trusted_cidrs = ip_cidr.split_list(trusted_cidrs_raw),
        real_ip_header = header,
    }
end

-- 读取环境变量 + 运行时文件，构建配置（供 handler / ban_check 启动时调用一次并缓存）
function _M.load_conf()
    local self_raw = os.getenv("SELF_PUBLIC_IPS") or ""
    local file_path = os.getenv("SELF_IP_FILE")
    if not file_path or file_path == "" then
        file_path = _M.DEFAULT_SELF_IP_FILE
    end
    local file_raw = _M.read_self_ip_file(file_path)
    if file_raw ~= "" then
        self_raw = self_raw .. "," .. file_raw
    end

    return _M.build_conf(self_raw, os.getenv("TRUSTED_PROXY_CIDRS") or "", os.getenv("REAL_IP_HEADER"))
end

-- 判断条目是否"可信"（私有 / 自身 / 可信段）——可信地址永不作为访客 IP
function _M.is_trusted(ip, conf)
    if not ip then return false end
    if ip_cidr.is_private_ip(ip) then return true end
    if conf.self_set[ip] then return true end
    for _, cidr in ipairs(conf.trusted_cidrs) do
        if ip_cidr.cidr_match(cidr, ip) then return true end
    end
    return false
end

-- 封禁 / 统计入口的"永不拉黑"判定（与 Java 侧 ServerSelfIpUtil.isNeverBanIp 语义对齐）
function _M.is_never_ban(ip, conf)
    if not ip or ip == "" then return true end
    return _M.is_trusted(ip, conf)
end

-- 从单值头（厂商头）中取最右侧有效条目（容忍被中间层错误追加成链的情况）
local function last_valid_entry(raw)
    local entries = ip_cidr.split_list(raw)
    for i = #entries, 1, -1 do
        local ip = ip_cidr.normalize_ip(entries[i])
        if ip then return ip end
    end
    return nil
end

-- 解析真实访客 IP
-- xff_raw      X-Forwarded-For 原始值（可为 nil）
-- vendor_raw   厂商真实客户端头原始值（REAL_IP_HEADER 对应的 $http_* 值，可为 nil）
-- remote_addr  TCP 对端（$remote_addr）
-- conf         build_conf / load_conf 产物
-- 返回：ip, source（source ∈ "vendor" / "xff" / "remote"，用于诊断日志）
function _M.resolve(xff_raw, vendor_raw, remote_addr, conf)
    -- 1) 厂商真实客户端头优先：仅当其非 XFF 且存在、且值不是可信地址（自身/私有/可信段）时采用。
    --    注意回环场景下厂商头也可能等于服务器自身 IP（CDN 把发起回环的服务器当成客户端），
    --    此时跳过它继续走 XFF 链，才能解析出更深的真实访客。
    local header = conf.real_ip_header
    if header and string.lower(header) ~= "x-forwarded-for" and vendor_raw and vendor_raw ~= "" then
        local v = last_valid_entry(vendor_raw)
        if v and not _M.is_trusted(v, conf) then
            return v, "vendor"
        end
    end

    -- 2) X-Forwarded-For 链：从右向左，跳过无效 / 私有 / 自身 / 可信段条目
    if xff_raw and xff_raw ~= "" then
        local entries = ip_cidr.split_list(xff_raw)
        for i = #entries, 1, -1 do
            local ip = ip_cidr.normalize_ip(entries[i])
            if ip and not _M.is_trusted(ip, conf) then
                return ip, "xff"
            end
        end
    end

    -- 3) 兜底：TCP 对端（整条链不可用；直连场景下即访客本身）
    local peer = ip_cidr.normalize_ip(remote_addr) or remote_addr
    return peer, "remote"
end

return _M
