-- HTTP 301 扫描守卫（rewrite 阶段执行）
--
-- 目标：保留 HTTP -> HTTPS 的 301 跳转，不牺牲正常访客体验，同时精确识别
-- "只用脚本 UA 高频打 HTTP、每次都吃 301" 的扫描器。
--
-- 判定（5 分钟固定窗口）：
--   1. 仅统计明确声明为脚本/自动化工具的 UA（浏览器 UA 一律不计数）；
--   2. 该 IP 脚本 UA 请求总数 >= 30（最少样本，避免一两次误判）；
--   3. HTTP 301 次数 >= 20；
--   4. 301 次数 / 该 IP 脚本 UA 请求总数 >= 90%。
--   按 90% 占比计算，最快在第 30 次请求（其中 27 次 301）时触发，而不是几百次以后。
-- 四个条件同时满足才写 1 小时黑名单；HTTP 请求由本脚本 444，HTTPS 由 ban_check 403。
--
-- 为什么不会误伤正常访客：
--   - 浏览器 UA 不进入任何计数；
--   - 正常浏览器只在首次访问时吃一个 301，之后全走 HTTPS，达不到 20 次 301；
--   - 已登录/管理员的浏览器请求同样因 UA 不是脚本工具而不计数；
--   - curl 健康检查、单次 API 调试的脚本请求量远低于阈值。
--
-- 搜索引擎蜘蛛：
--   - 蜘蛛不是浏览器，不缓存 HSTS，每次抓 http:// 都会产生一次 301；29% 左右的
--     301 占比是正常形态，不能当扫描器；
--   - 已知蜘蛛 UA 先读 Java SearchEngineVerifier 的 Redis 验证结果：
--       verified -> 放行不计数
--       failed -> 按脚本 UA 计数（伪装蜘蛛）
--       pending / 无缓存 -> 按“未验证蜘蛛”严格阈值计数（50 次请求 / 40 次 301 / 95%）
--   - 真蜘蛛实测 301 率约 29%，低于 95%，因此不会触发未验证蜘蛛阈值；
--   - UA 混拼多个搜索引擎（BaiduSpider,Sogou,Googlebot 等）直接按脚本处理；
--   - 不做 Nginx 本地 PTR 查询：Java 已有带并发限流和 Redis 缓存的 FCrDNS，
--     边缘重复实现会引入 DNS 超时和解析链路差异。
--
-- 适用范围（重要，勿误判为“守卫失效”）：
--   本脚本由 rewrite_entry.lua 调用，只覆盖“请求真正到达本容器”的流量。分两种部署形态：
--     1) 容器的 80/443 直接对外（标准 poetize 脚本部署）：公网 301 由本脚本发出，全部可见；
--     2) 80/443 由前置代理（1Panel / 其它 OpenResty / CDN）持有，并在代理层直接
--        return 301：公网 301 在代理层就结束，本脚本看不到这部分，只覆盖直连本容器端口的
--        流量（内部调用、绕过代理的扫描器、联调测试）。
--   形态 2 属预期行为，不是配置错误，也无需在代理层重复挂载本脚本。理由：
--     - 代理层返回 301 不触碰后端，单次成本只有几十字节响应头；
--     - 真正有害的“301 污染访问统计”已由 nginx.conf 的 page_visit_status_ok 只认 2xx
--       与 Java 消费端双重排除，与本脚本是否看到请求无关；
--     - 在宿主机代理层挂脚本需要额外解决 Lua 依赖、Redis 连通与面板更新覆盖，
--       收益远小于维护成本，且失败时为静默 fail-open。
--   生产实测（形态 2）：公网 301 全部由前置代理返回；容器 access.log 中仍有直连端口的
--   301，且其中扫描器占多数，因此保留本守卫对这部分流量仍然有效。
--
-- 为什么挂在 rewrite 而不是 log：
--   1. server 块的 `return 301` 属于 SERVER_REWRITE，会先于 REWRITE 阶段的 Lua 结束请求；
--      因此本脚本现在自己负责发出 301，不能再用 server 级 return；
--   2. ngx_lua 的 cosocket（resty.redis）在 log_by_lua 阶段被禁用，放在 log 阶段会
--      直接 fail-open，表现为“配置存在但永远不生效”。
--
-- 设计约束：
--   - fail-open：Redis 不可用、IP 无法识别时仍然正常跳转 HTTPS；
--   - 永不对服务器自身 / 可信代理 / 非公网 IP 写黑名单（与 Java/Lua 其它入口一致）；
--   - 只有脚本/蜘蛛 UA 访问 Redis，普通浏览器直接 301，零 Redis 开销。

local redis = require "resty.redis"
local cjson = require "cjson.safe"

-- 由 docker-compose 注入环境变量，经 nginx.conf 的 env 指令暴露给 Lua。
-- 必须与 Java 端 spring.data.redis 的 host/port/database 保持一致。
local REDIS_HOST = os.getenv("REDIS_HOST") or "redis"
local REDIS_PORT = tonumber(os.getenv("REDIS_PORT") or "6379") or 6379
local REDIS_PASSWORD = os.getenv("REDIS_PASSWORD") or ""
local REDIS_DB = tonumber(os.getenv("REDIS_DB") or "0") or 0

local CONNECT_TIMEOUT = tonumber(os.getenv("REDIS_CONNECT_TIMEOUT_MS") or "500") or 500
local READ_TIMEOUT = tonumber(os.getenv("REDIS_READ_TIMEOUT_MS") or "500") or 500
local POOL_SIZE = 50
local POOL_MAX_IDLE = 10000

local WINDOW_SECONDS = 300
local COUNTER_TTL_SECONDS = 600
-- 先攒够总样本再判定：避免"前几次全是 301，随后才出现正常 HTTPS 200"
-- 这种序列在阈值刚达到时被误判。30 个脚本请求已经足够证明流量形态，
-- 按 90% 占比最快第 30 次请求（其中 27 次 301）即触发，高流量扫描器通常几秒内就会被封。
local MIN_TOTAL_COUNT = 30
local MIN_301_COUNT = 20
local MIN_301_RATE_PERCENT = 90
-- 未完成 Java 反查的蜘蛛 UA 先按更严格阈值计数：
-- 真蜘蛛实测 301 率约 29%，远低于 95%；GCP 那类纯 301 伪造才会命中。
local UNVERIFIED_SPIDER_MIN_TOTAL = 50
local UNVERIFIED_SPIDER_MIN_301 = 40
local UNVERIFIED_SPIDER_RATE_PERCENT = 95
local BAN_SECONDS = 3600

local COUNTER_PREFIX = "poetize:security:http301:"
local BLACKLIST_PREFIX = "poetize:security:blacklist:"

-- 明确声明自动化工具/脚本运行时的 UA 关键词（小写，plain contains）。
-- 只放高置信度标识；浏览器发起的 axios/fetch 请求仍携带浏览器 UA，不会命中。
local SCRIPT_UA_KEYWORDS = {
    "curl/", "wget/", "python-requests", "python-urllib", "aiohttp/", "httpx/",
    "go-http-client", "java/", "okhttp/", "apache-httpclient", "libwww-perl",
    "lwp-trivial", "node-fetch", "axios/", "powershell/", "postmanruntime/",
    "insomnia/", "nuclei", "sqlmap", "nikto", "masscan", "zgrab", "nmap",
    "gobuster", "ffuf", "feroxbuster", "dirbuster",
}

-- 已知搜索引擎 UA 与 Java SearchEngineVerifier 的 cacheName 对应关系。
-- 蜘蛛正常抓取会产生 301（http 首跳），不能把它当脚本扫描器；但也不直接信任 UA，
-- 只读取 Java 反向 DNS 验证结果：
--   verified -> 不计数
--   failed   -> 按脚本 UA 计数（伪装蜘蛛）
--   pending / 无缓存 -> 按“未验证蜘蛛”严格阈值计数（50 次请求 / 40 次 301 / 95%）
-- 一个 UA 同时声明多个搜索引擎（如 BaiduSpider,Sogou,Googlebot 混拼）直接按脚本处理。
local SEARCH_ENGINE_UA_RULES = {
    { token = "googlebot", cache = "googlebot" },
    { token = "baiduspider", cache = "baiduspider" },
    { token = "bingbot", cache = "bingbot" },
    { token = "yahoo! slurp", cache = "yahooslurp" },
    { token = "yahoo slurp", cache = "yahooslurp" },
    { token = "slurp", cache = "yahooslurp" },
    { token = "sogou", cache = "sogouspider" },
    { token = "360spider", cache = "360spider" },
    { token = "haosouspider", cache = "360spider" },
    { token = "yandexbot", cache = "yandexbot" },
    { token = "duckduckbot", cache = "duckduckbot" },
    { token = "bytespider", cache = "bytespider" },
    { token = "yisouspider", cache = "yisouspider" },
    { token = "petalbot", cache = "petalbot" },
    { token = "applebot", cache = "applebot" },
    { token = "seznambot", cache = "seznambot" },
}

local SEARCH_BOT_VERIFY_PREFIX = "poetize:visit:bot-verify:"

-- 健康检查 / ACME 验证不计入，避免监控与证书续期被当成扫描。
local EXEMPT_URI_EXACT = {
    ["/actuator/health"] = true,
    ["/actuator/health/liveness"] = true,
    ["/actuator/health/readiness"] = true,
}

local real_ip_core = nil
do
    local ok, mod = pcall(require, "real_ip_core")
    if ok and mod then
        real_ip_core = mod
    else
        ngx.log(ngx.WARN, "http_301_guard: 加载 real_ip_core.lua 失败，自身 IP 豁免不可用: ", mod)
    end
end

local never_ban_conf = nil

local function is_never_ban_ip(ip)
    if not real_ip_core or not ip or ip == "" then
        return false
    end
    if never_ban_conf == nil then
        local ok, conf = pcall(real_ip_core.load_conf)
        never_ban_conf = (ok and conf) or false
    end
    if not never_ban_conf then
        return false
    end
    local ok, result = pcall(real_ip_core.is_never_ban, ip, never_ban_conf)
    return ok and result or false
end

local function get_client_ip()
    local ip = ngx.var.real_client_ip
    if ip and ip ~= "" and ip ~= "unknown" then
        return ip
    end
    return ngx.var.remote_addr
end

local function is_script_ua(ua)
    if not ua or ua == "" then
        return false
    end
    local lower = string.lower(ua)
    for _, keyword in ipairs(SCRIPT_UA_KEYWORDS) do
        if string.find(lower, keyword, 1, true) then
            return true
        end
    end
    return false
end

-- 返回：cacheName, 是否同时声明多个不同搜索引擎
local function detect_search_engine_ua(ua)
    if not ua or ua == "" then
        return nil, false
    end
    local lower = string.lower(ua)
    local seen = {}
    local first_cache = nil
    local distinct = 0
    for _, rule in ipairs(SEARCH_ENGINE_UA_RULES) do
        if string.find(lower, rule.token, 1, true) and not seen[rule.cache] then
            seen[rule.cache] = true
            distinct = distinct + 1
            if not first_cache then
                first_cache = rule.cache
            end
        end
    end
    return first_cache, distinct > 1
end

-- 读取 Java SearchEngineVerifier 的验证结果。
-- Redis 值来自 RedisUtil 的 JSON 序列化，可能是 "\"verified|...\""，这里同时兼容裸值。
local function read_bot_verification(red, cache_name, ip)
    local value, err = red:get(SEARCH_BOT_VERIFY_PREFIX .. cache_name .. ":" .. ip)
    if not value then
        return nil, err
    end
    if value == ngx.null then
        return nil
    end
    local text = tostring(value)
    if string.sub(text, 1, 1) == '"' then
        text = string.sub(text, 2, -2)
    end
    local separator = string.find(text, "|", 1, true)
    local status = separator and string.sub(text, 1, separator - 1) or text
    return string.lower(status)
end

local function is_acme_uri(uri)
    if not uri then
        return false
    end
    local acme_prefix = "/.well-known/acme-challenge/"
    return string.sub(uri, 1, #acme_prefix) == acme_prefix
end

local function is_exempt_uri(uri)
    if not uri then
        return false
    end
    return EXEMPT_URI_EXACT[uri] == true or is_acme_uri(uri)
end

local function get_redis_connection()
    local red = redis:new()
    red:set_timeout(CONNECT_TIMEOUT, READ_TIMEOUT, READ_TIMEOUT)

    local ok, err = red:connect(REDIS_HOST, REDIS_PORT)
    if not ok then
        ngx.log(ngx.WARN, "http_301_guard: Redis connect failed: ", err)
        return nil
    end

    if REDIS_PASSWORD and REDIS_PASSWORD ~= "" then
        local auth_ok, auth_err = red:auth(REDIS_PASSWORD)
        if not auth_ok then
            ngx.log(ngx.ERR, "http_301_guard: Redis auth failed: ", auth_err)
            red:close()
            return nil
        end
    end

    if REDIS_DB and REDIS_DB ~= 0 then
        local select_ok, select_err = red:select(REDIS_DB)
        if not select_ok then
            ngx.log(ngx.WARN, "http_301_guard: Redis select db failed: ", select_err)
            red:close()
            return nil
        end
    end

    return red
end

local function release_redis(red)
    if red then
        red:set_keepalive(POOL_MAX_IDLE, POOL_SIZE)
    end
end

-- 原子累加：total 必加，301 仅在当前响应为 301 时加。
local INCR_BOTH_SCRIPT = [[
local total = redis.call('INCR', KEYS[1])
if total == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
-- 非 301 请求也必须读取已有 301 计数，否则第 30 次请求若是 HTTPS 200
-- 会把 hit 当成 0，导致 "27 次 301 + 3 次 200" 永远无法判定。
local hit = tonumber(redis.call('GET', KEYS[2]) or '0')
if ARGV[2] == '1' then
    hit = redis.call('INCR', KEYS[2])
    if hit == 1 then
        redis.call('EXPIRE', KEYS[2], ARGV[1])
    end
end
return {total, hit}
]]

-- 黑名单写入必须原子且只在新 key 时设置，避免 EXISTS→SET 并发覆盖更长的 24h/7d/永久封禁。
-- 值用 JSON 字符串（带引号），与 Java RedisUtil 的 String 序列化格式一致；
-- 否则管理端 listBlacklistedIps 用 JSON 反序列化读裸串会失败并删除 key。
local SET_BLACKLIST_IF_ABSENT_SCRIPT = [[
if redis.call('EXISTS', KEYS[1]) == 1 then
    return 0
end
redis.call('SETEX', KEYS[1], ARGV[2], ARGV[1])
return 1
]]

local function record_script_request(red, ip, is_301, min_total, min_hit, min_rate)
    local bucket = math.floor(ngx.time() / WINDOW_SECONDS)
    local base = COUNTER_PREFIX .. ip .. ":" .. bucket
    local total_key = base .. ":total"
    local hit_key = base .. ":301"

    local result, err = red:eval(
            INCR_BOTH_SCRIPT, 2, total_key, hit_key,
            COUNTER_TTL_SECONDS, is_301 and "1" or "0")
    if not result then
        ngx.log(ngx.WARN, "http_301_guard: Redis eval failed: ", err)
        return
    end

    local total = tonumber(result[1]) or 0
    local hit = tonumber(result[2]) or 0
    -- 在任意脚本请求上判定，而不是只在 301 上判定：否则 "27 次 301 + 3 次 200"
    -- 在总数刚达到 30 时没有 301 再触发检查，会漏掉真实的高 301 占比。
    if total < min_total or hit < min_hit then
        return
    end
    if hit * 100 < total * min_rate then
        return
    end

    local blacklist_key = BLACKLIST_PREFIX .. ip
    local reason = cjson.encode("auto:http-301-scan:" .. ngx.localtime())
    if not reason then
        reason = '"auto:http-301-scan"'
    end

    local set_ok, set_err = red:eval(SET_BLACKLIST_IF_ABSENT_SCRIPT, 1,
            blacklist_key, reason, BAN_SECONDS)
    if not set_ok then
        ngx.log(ngx.WARN, "http_301_guard: Redis blacklist eval failed: ", set_err)
        return
    end
    -- 已有封禁（可能是更长的 24h/7d/永久）不覆盖、不缩短
    if tonumber(set_ok) ~= 1 then
        return
    end
    ngx.log(ngx.WARN, "http_301_guard: banned IP=", ip,
            " 301=", hit, " total=", total, " window=", WINDOW_SECONDS, "s")
end

local function redirect_https()
    local host = ngx.var.host
    if not host or host == "" then
        host = ngx.var.http_host or ""
    end
    return ngx.redirect("https://" .. host .. ngx.var.request_uri, 301)
end

local function main()
    -- 只有 HTTP server 会把当前请求变成 301；HTTPS 请求仅作为占比分母。
    local is_http_301 = ngx.var.scheme == "http"
    local uri = ngx.var.uri

    -- ACME 验证必须留在 HTTP 本地响应，不能跳 HTTPS
    if is_http_301 and is_acme_uri(uri) then
        return
    end
    -- 健康检查不计数，但 HTTP 健康检查仍保持原来的 301 行为
    if is_exempt_uri(uri) then
        if is_http_301 then
            return redirect_https()
        end
        return
    end

    local ua = ngx.var.http_user_agent
    local script_ua = is_script_ua(ua)
    local spider_cache, multiple_spider_claims = detect_search_engine_ua(ua)

    -- 一个 UA 混拼多个搜索引擎（如 BaiduSpider,Sogou,Googlebot）不是正常蜘蛛，直接按脚本处理。
    if multiple_spider_claims then
        script_ua = true
        spider_cache = nil
    end

    if not script_ua and not spider_cache then
        if is_http_301 then
            return redirect_https()
        end
        return
    end

    local ip = get_client_ip()
    if not ip or ip == "" or ip == "unknown" or is_never_ban_ip(ip) then
        if is_http_301 then
            return redirect_https()
        end
        return
    end

    local red = get_redis_connection()
    if not red then
        if is_http_301 then
            return redirect_https()
        end
        return
    end

    -- 声称是蜘蛛的 UA：只信 Java 反向 DNS 的明确 verified 结论。
    -- verified -> 放行；failed -> 按普通脚本阈值；pending/无缓存 -> 按更严格的
    -- “未验证蜘蛛”阈值计数，用实测 29% 的蜘蛛 301 率做安全边界。
    local unverified_spider = false
    if spider_cache and not script_ua then
        local status, status_err = read_bot_verification(red, spider_cache, ip)
        if not status and status_err then
            -- Redis 读取失败：fail-open，不计数也不封禁
            ngx.log(ngx.WARN, "http_301_guard: bot verification read failed: ", status_err)
            release_redis(red)
            if is_http_301 then
                return redirect_https()
            end
            return
        end
        if status == "verified" then
            release_redis(red)
            if is_http_301 then
                return redirect_https()
            end
            return
        elseif status == "failed" then
            ngx.log(ngx.INFO, "http_301_guard: spoofed search engine UA detected, engine=", spider_cache,
                    " ip=", ip)
        else
            -- pending / 无缓存：未验证蜘蛛，走更严格阈值
            unverified_spider = true
        end
    end

    -- HTTP 的 301 由本脚本在统计/封禁后发出，access 阶段的 ban_check 不会运行，
    -- 因此这里必须先检查一次黑名单，避免已封 IP 继续吃 301。
    if is_http_301 then
        local exists, exists_err = red:exists(BLACKLIST_PREFIX .. ip)
        if not exists then
            -- 查询失败时不继续 SET，避免把可能存在的 24h/7d/永久封禁覆盖成 1h。
            ngx.log(ngx.WARN, "http_301_guard: Redis exists failed: ", exists_err)
            release_redis(red)
            if is_http_301 then
                return redirect_https()
            end
            return
        elseif exists == 1 then
            release_redis(red)
            -- 444 直接断连：不告诉扫描器“被识别了”，也省掉响应体。
            ngx.exit(444)
        end
    end

    local min_total = MIN_TOTAL_COUNT
    local min_hit = MIN_301_COUNT
    local min_rate = MIN_301_RATE_PERCENT
    if unverified_spider then
        min_total = UNVERIFIED_SPIDER_MIN_TOTAL
        min_hit = UNVERIFIED_SPIDER_MIN_301
        min_rate = UNVERIFIED_SPIDER_RATE_PERCENT
    end

    local ok, err = pcall(record_script_request, red, ip, is_http_301, min_total, min_hit, min_rate)
    if not ok then
        ngx.log(ngx.WARN, "http_301_guard: record failed: ", err)
    end
    release_redis(red)

    if is_http_301 then
        return redirect_https()
    end
end

main()
