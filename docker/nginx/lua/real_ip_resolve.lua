-- rewrite 阶段执行：解析真实访客 IP 并写入 $real_client_ip_resolved
--
-- 由各 server 块中的 `rewrite_by_lua_file` 调用；解析结果经 nginx.conf 的 map
-- 暴露为 $real_client_ip（下游 proxy_set_header / 日志 / ban_check.lua 统一引用），
-- 未写入时 map 回退 $remote_addr（TCP 对端），任何失败都不阻断请求。
--
-- 配置（环境变量，声明见 nginx.conf 顶部 env 区）：
--   SELF_PUBLIC_IPS / SELF_IP_FILE / TRUSTED_PROXY_CIDRS / REAL_IP_HEADER
-- 详见 real_ip_core.lua 头注释。

local core = require "real_ip_core"

-- 每 worker 缓存一次配置（环境变量与运行时文件在容器启动时即固定）
local conf = nil
local function get_conf()
    if conf == nil then
        local ok, result = pcall(core.load_conf)
        conf = (ok and result) or false
    end
    return conf or nil
end

-- 按头名查请求头：X-Real-IP → ngx.var.http_x_real_ip
local function get_header(name)
    if not name or name == "" then return nil end
    local key = "http_" .. string.lower((name:gsub("%-", "_")))
    local ok, value = pcall(function() return ngx.var[key] end)
    if ok then return value end
    return nil
end

local function main()
    local c = get_conf()
    if not c then return end

    local resolved = core.resolve(
        ngx.var.http_x_forwarded_for,
        get_header(c.real_ip_header),
        ngx.var.remote_addr,
        c
    )
    if resolved and resolved ~= "" then
        -- 变量由 server 块的 `set $real_client_ip_resolved '';` 声明；
        -- 未声明（自定义配置未接线）时赋值会抛错，静默忽略即可（下游回退 $remote_addr）
        pcall(function() ngx.var.real_client_ip_resolved = resolved end)
    end
end

-- 任何异常都不能让 rewrite 阶段失败（否则请求直接 500）：
-- 解析失败时静默返回，$real_client_ip 回退 $remote_addr，语义安全。
pcall(main)
