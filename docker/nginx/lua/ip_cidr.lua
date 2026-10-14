-- IP / CIDR 共享工具模块（纯函数，无 ngx 依赖，可离线单测）
--
-- 使用方：
--   1. ban_check.lua         —— CIDR 网段封禁匹配（原先内联在本文件，现收敛到此处，避免双份副本漂移）
--   2. real_ip_core.lua      —— 真实访客 IP 解析（私有段判定 / 可信段匹配）
--
-- 注意：
--   * cidr_match 的行为与旧 ban_check.lua 内联实现逐行一致（IPv4/IPv6、:: 缩写展开、
--     32 位有符号运算掩码），改动需回归 ban_check 的地区/CIDR 封禁路径。
--   * 公开的 normalize_ip 用于把代理链里的条目清洗为纯 IP 字面量：
--     去空白、去端口（[v6]:port / v4:port）、v4-mapped（::ffff:a.b.c.d → a.b.c.d）、
--     非法条目返回 nil。

local bit = require "bit"

local _M = {}

-- ========== IPv4 / IPv6 基础解析 ==========

-- IPv4 解析为 uint32
local function ipv4_to_uint32(ip)
    local a, b, c, d = ip:match("^(%d+)%.(%d+)%.(%d+)%.(%d+)$")
    if not a then return nil end
    a, b, c, d = tonumber(a), tonumber(b), tonumber(c), tonumber(d)
    if not a or not b or not c or not d then return nil end
    if a > 255 or b > 255 or c > 255 or d > 255 then return nil end
    return a * 16777216 + b * 65536 + c * 256 + d
end

-- IPv6 解析为 4 个 uint32（8 组 16bit 合并为 4 组 32bit）
-- 处理 :: 缩写，返回 {g0, g1, g2, g3} 或 nil
local function ipv6_to_uint32_groups(ip)
    if not ip then return nil end

    -- 检测并拆分 ::
    local left_str, right_str
    local dc_start, dc_end = ip:find("::")
    if dc_start then
        -- 仅允许一个 ::
        if ip:find("::", dc_end + 1) then
            return nil
        end
        left_str = ip:sub(1, dc_start - 1)
        right_str = ip:sub(dc_end + 1)
    else
        left_str = ip
        right_str = ""
    end

    -- 按冒号切分（忽略空段）
    local function split_groups(s)
        local t = {}
        if s and s ~= "" then
            for g in s:gmatch("([^:]+)") do
                table.insert(t, g)
            end
        end
        return t
    end

    local left_groups = split_groups(left_str)
    local right_groups = split_groups(right_str)
    local total = #left_groups + #right_groups

    if dc_start then
        -- 含 ::，:: 至少展开一组零，非零段最多 7 个
        if total > 7 then return nil end
    else
        -- 不含 ::，必须是 8 组
        if total ~= 8 then return nil end
    end

    -- 中间填零展开为 8 组
    local zeros_count = 8 - total
    local groups = {}
    for _, g in ipairs(left_groups) do table.insert(groups, g) end
    for _ = 1, zeros_count do table.insert(groups, "0") end
    for _, g in ipairs(right_groups) do table.insert(groups, g) end

    if #groups ~= 8 then return nil end

    -- 每 2 组 16bit 合并为 1 个 uint32
    local uint32_groups = {}
    for i = 1, 8, 2 do
        local hi = tonumber(groups[i], 16)
        local lo = tonumber(groups[i + 1], 16)
        if not hi or not lo then return nil end
        if hi > 0xFFFF or lo > 0xFFFF then return nil end
        table.insert(uint32_groups, hi * 65536 + lo)
    end

    return uint32_groups
end

-- CIDR 匹配（支持 IPv4 和 IPv6）
-- cidr_str: "IP/prefix"，例如 "192.168.1.0/24" 或 "2001:db8::/32"
-- ip: 客户端 IP
-- 返回 true 表示命中
-- 注意：LuaJIT bit 模块为 32 位有符号整数运算，band/rshift/bnot 对 32 位模式仍正确
local function cidr_match(cidr_str, ip)
    local slash = cidr_str:find("/")
    if not slash then return false end
    local cidr_ip = cidr_str:sub(1, slash - 1)
    local prefix_len = tonumber(cidr_str:sub(slash + 1))
    if not prefix_len or prefix_len < 0 then return false end

    if cidr_ip:find(":") then
        -- IPv6
        if not ip:find(":") then return false end  -- 类型不匹配
        if prefix_len > 128 then return false end
        local cidr_groups = ipv6_to_uint32_groups(cidr_ip)
        local ip_groups = ipv6_to_uint32_groups(ip)
        if not cidr_groups or not ip_groups then return false end

        if prefix_len == 0 then return true end  -- 匹配所有 IPv6

        -- 分组比较：每组 32 位，>=32 精确比较，<32 用掩码，==0 跳过
        local remaining = prefix_len
        for i = 1, 4 do
            if remaining <= 0 then break end
            local group_prefix
            if remaining >= 32 then
                group_prefix = 32
            else
                group_prefix = remaining
            end

            if group_prefix == 32 then
                -- 整组精确比较
                if cidr_groups[i] ~= ip_groups[i] then return false end
            elseif group_prefix == 0 then
                -- 跳过该组（保险分支，正常流程不会到达）
            else
                local mask = bit.bnot(bit.rshift(0xFFFFFFFF, group_prefix))
                if bit.band(cidr_groups[i], mask) ~= bit.band(ip_groups[i], mask) then
                    return false
                end
            end
            remaining = remaining - 32
        end
        return true
    else
        -- IPv4
        if ip:find(":") then return false end  -- 类型不匹配
        if prefix_len > 32 then return false end
        local cidr_uint = ipv4_to_uint32(cidr_ip)
        local ip_uint = ipv4_to_uint32(ip)
        if not cidr_uint or not ip_uint then return false end
        if prefix_len == 0 then return true end          -- 匹配所有 IPv4
        if prefix_len >= 32 then return ip_uint == cidr_uint end  -- /32 精确匹配
        local mask = bit.bnot(bit.rshift(0xFFFFFFFF, prefix_len))
        return bit.band(ip_uint, mask) == bit.band(cidr_uint, mask)
    end
end

-- ========== 条目清洗 / 列表解析 / 私有段判定 ==========

-- 把代理链条目清洗为纯 IP 字面量，非法返回 nil
-- 支持：去首尾空白；[v6]:port / v4:port 去端口；::ffff:a.b.c.d → a.b.c.d
function _M.normalize_ip(raw)
    if raw == nil then return nil end

    local ip = raw:match("^%s*(.-)%s*$")
    if ip == "" then return nil end

    -- [2001:db8::1]:443 → 2001:db8::1
    local bracket = ip:match("^%[([^%]]+)%]:%d+$")
    if bracket then
        ip = bracket
    else
        -- 1.2.3.4:5678 → 1.2.3.4（裸 IPv6 末段为数字时不视为端口，避免误伤）
        if ip:match("^%d+%.%d+%.%d+%.%d+:%d+$") then
            ip = ip:gsub(":%d+$", "")
        end
    end

    -- v4-mapped：::ffff:1.2.3.4 → 1.2.3.4
    local mapped = ip:match("^::ffff:(%d+%.%d+%.%d+%.%d+)$")
    if mapped then
        ip = mapped
    end

    -- 用解析器做最终校验（同时拒绝主机名、垃圾串）
    if ip:find(":", 1, true) then
        if ipv6_to_uint32_groups(ip) then return ip end
        return nil
    end
    if ipv4_to_uint32(ip) then return ip end
    return nil
end

-- 解析逗号/分号/空白/换行分隔的配置字符串为列表（保持顺序）
function _M.split_list(s)
    local out = {}
    if not s or s == "" then return out end
    for piece in s:gmatch("[^,;%s]+") do
        out[#out + 1] = piece
    end
    return out
end

-- 私有 / 回环 / 保留 / 链路本地 网段（永不可能作为"访客公网 IP"）
local PRIVATE_CIDRS = {
    "0.0.0.0/8",       -- This network
    "10.0.0.0/8",      -- 私有
    "100.64.0.0/10",   -- CGNAT
    "127.0.0.0/8",     -- 回环
    "169.254.0.0/16",  -- 链路本地
    "172.16.0.0/12",   -- 私有
    "192.0.0.0/24",    -- IANA Special-Purpose
    "192.0.2.0/24",    -- TEST-NET-1 (RFC 5737)
    "192.88.99.0/24",  -- 6to4 中继
    "192.168.0.0/16",  -- 私有
    "198.18.0.0/15",   -- 基准测试 (RFC 2544)
    "198.51.100.0/24", -- TEST-NET-2
    "203.0.113.0/24",  -- TEST-NET-3
    "224.0.0.0/4",     -- 组播
    "240.0.0.0/4",     -- 保留
    "::/128",          -- 未指定
    "::1/128",         -- 回环
    "fc00::/7",        -- ULA
    "fe80::/10",       -- 链路本地
    "ff00::/8",        -- 组播
    "2001:db8::/32",   -- 文档 (RFC 3849)
}

-- 判断是否为私有 / 回环 / 保留 / 链路本地 IP（非法 IP 返回 true，视为不可作为访客）
function _M.is_private_ip(ip)
    if not ip or ip == "" then return true end
    if not (ipv4_to_uint32(ip) or ipv6_to_uint32_groups(ip)) then
        return true
    end
    for _, cidr in ipairs(PRIVATE_CIDRS) do
        if cidr_match(cidr, ip) then
            return true
        end
    end
    return false
end

_M.ipv4_to_uint32 = ipv4_to_uint32
_M.ipv6_to_uint32_groups = ipv6_to_uint32_groups
_M.cidr_match = cidr_match

return _M
