-- rewrite 阶段唯一入口。
--
-- ngx_lua 的大多数 *_by_lua* 指令不允许在同一个 server/location context 重复出现，
-- 两条 rewrite_by_lua_file 会导致 `nginx: [emerg] directive is duplicate`。
-- 这里把 real_ip_resolve 和 http_301_guard 合并成一个入口，按顺序执行：
--   1. 解析真实访客 IP（脚本内部 pcall，失败回退 remote_addr）
--   2. 301 扫描守卫（可能计数、封禁、444，或对 HTTP 发出 301）
--
-- 两个被调用脚本都通过 loadfile 编译一次并缓存在 package.loaded，
-- 后续请求只执行 chunk，不重复读文件。

local ENTRY_KEY = "poetize.rewrite_entry"
local cached = package.loaded[ENTRY_KEY]

if not cached then
    local real_ip_path = "/usr/local/openresty/nginx/lua/real_ip_resolve.lua"
    local guard_path = "/usr/local/openresty/nginx/lua/http_301_guard.lua"

    local real_ip_chunk, real_ip_err = loadfile(real_ip_path)
    if not real_ip_chunk then
        ngx.log(ngx.ERR, "rewrite_entry: 加载 real_ip_resolve 失败: ", real_ip_err)
        return
    end

    local guard_chunk, guard_err = loadfile(guard_path)
    if not guard_chunk then
        ngx.log(ngx.ERR, "rewrite_entry: 加载 http_301_guard 失败: ", guard_err)
        return
    end

    cached = function()
        -- real_ip_resolve 内部已经 pcall(main)，不会把 rewrite 阶段打挂
        real_ip_chunk()
        -- 不包 pcall：ngx.exit/ngx.redirect 依赖 ngx_lua 的特殊控制流
        guard_chunk()
    end
    package.loaded[ENTRY_KEY] = cached
end

cached()
