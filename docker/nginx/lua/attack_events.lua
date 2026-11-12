-- 通用接入层攻击事件。OpenResty 1.29.2.5 / LuaJIT 2.1；不依赖二进制补丁之外的新模块。
-- log 阶段只归因并入有界 worker 队列，cosocket 由周期 timer 执行（log 阶段禁止 cosocket）。
-- 原生日志只匹配消息开头的白名单特征，不匹配 request/UA 等攻击者可控日志后缀。
-- 计数为 Redis 原子时间戳队列：精确滑动窗口，最多保留 threshold+1 项；过期清理摊销 O(1)。
local redis = require "resty.redis"
local cjson = require "cjson.safe"
local errlog = require "ngx.errlog"
local core = require "real_ip_core"
local ip_util = require "ip_cidr"
local M = {}
local cache = ngx.shared.attack_events

local function enabled(name, default)
    local value = os.getenv("NGINX_ATTACK_" .. name)
    if not value or value == "" then return default end
    value = string.lower(value)
    return value == "true" or value == "1" or value == "on"
end
local function number(name, default, min, max)
    local value = tonumber(os.getenv("NGINX_ATTACK_" .. name)) or default
    return math.max(min, math.min(max, math.floor(value)))
end
local active = enabled("ENABLED", true)
local window = number("WINDOW_SECONDS", 60, 1, 3600)
local ban_ttl = number("BAN_SECONDS", 600, 1, 86400)
local events = {
    E1 = { on = enabled("E1_ENABLED", true), threshold = number("E1_THRESHOLD", 10, 1, 4096) },
    E2 = { on = enabled("E2_ENABLED", true), observe = true,
           threshold = number("E2_THRESHOLD", 20, 1, 4096) },
    E3 = { on = enabled("E3_ENABLED", true), observe = true,
           threshold = number("E3_THRESHOLD", 20, 1, 4096) },
    E4 = { on = enabled("E4_ENABLED", true), threshold = number("E4_THRESHOLD", 20, 1, 4096) },
    E5 = { on = enabled("E5_ENABLED", true), threshold = number("E5_THRESHOLD", 10, 1, 4096) },
    E6 = { on = enabled("E6_ENABLED", false), threshold = number("E6_THRESHOLD", 120, 1, 4096) },
}
local conf
local queue, head, tail = {}, 1, 0
local QUEUE_LIMIT, BATCH_SIZE = 1024, 128
local COUNTER_PREFIX = "poetize:security:events:"
local BLACKLIST_PREFIX = "poetize:security:blacklist:"
local backoff_until = 0

-- E2/E3 只观测：连接层错误没有可靠的请求身份，禁止拿 client: 对端地址封禁。
-- 后续新增事件只需注册上表 + native_feature/log_request 的行为特征，复用下列执行器。
local function native_feature(line)
    -- 只接受 nginx 原生行；Lua 日志正文或请求内容不能伪造另一个事件。
    local connection, message = line:match("^%d%d%d%d/%d%d/%d%d %d%d:%d%d:%d%d %[%w+%] %d+#%d+: %*(%d+) (.*)")
    if not message or message:sub(1, 5) == "[lua]" then return end
    if message:find("^client sent too many header lines") then
        return "E1", connection, "too_many_headers"
    end
    if message:find("^client sent too long URI") then
        return "E5", connection, "uri_too_long"
    end
    if message:find("^client sent too long header line")
       or message:find("^client sent too large request") then
        return "E5", connection, "headers_too_large"
    end
    if message:find("^client sent too large header")
       or message:find("^client sent too many PRIORITY frames")
       or message:find("^client sent too many refused streams") then
        if message:find(" while processing HTTP/2 connection", 1, true) then
            return "E3", connection, "h2_connection_rejected"
        end
    end
    if message:find("^client sent invalid ") then
        return "E4", connection, "invalid_request"
    end
    -- 只匹配明确的协议异常，不把普通 QUIC 超时/握手失败当攻击。
    if message:find("^quic (.+) is invalid") or message:find("^quic flood detected")
       or message:find("^quic too short dcid in initial")
       or message:find("^quic trailing garbage in payload") then
        return "E2", connection, "quic_protocol_rejected"
    end
end

local function log(event, ip, action, reason, count)
    ngx.log(ngx.WARN, "attack_event ", cjson.encode({event = event, ip = ip or "unknown",
        action = action, reason = reason, count = count or 0,
        window_seconds = window, ban_seconds = ban_ttl}))
end

local function limited_log(event, ip, action, reason)
    -- shared dict 只用于限制重复诊断日志，不保存权威计数/封禁；满时允许淘汰诊断键。
    if not cache or cache:add(action .. ":" .. event .. ":" .. (ip or "unknown"), true, 60) then
        log(event, ip, action, reason)
    end
end

local function drain_native(connection)
    local matched
    -- 每次最多取 128 条，固定上限；ring 满时会覆盖旧日志，宁可漏报也不猜原因封禁。
    local logs, err = errlog.get_logs(128)
    if not logs then
        limited_log("system", nil, "error", "capture_unavailable:" .. tostring(err))
        return
    end
    for i = 1, #logs, 3 do
        local line = logs[i + 2]
        local event, id, reason = native_feature(line)
        if event then
            if events[event].observe then
                if events[event].on then
                    local peer = line:match(", client: ([^, ]+)") or "unknown"
                    limited_log(event, nil, "observe", reason .. ":peer=" .. peer)
                end
            elseif id == connection then
                -- 关闭某类采集后仍保留分类，避免被 E6 当作普通 400 再次计数。
                matched = {event = event, reason = reason}
            end
        end
    end
    return matched
end

local function header(name)
    local key = "http_" .. string.lower((name:gsub("%-", "_")))
    local ok, value = pcall(function() return ngx.var[key] end)
    return ok and value or nil
end

local function request_ip()
    -- 早期 400/414 在 rewrite 前结束，set 变量尚未初始化。
    -- 用完全相同的 core.resolve 重算，并回写统一变量，后续 access log 也能得到真实 IP。
    local ip, source = core.resolve(ngx.var.http_x_forwarded_for, header(conf.real_ip_header),
        ngx.var.remote_addr, conf)
    pcall(function() ngx.var.real_client_ip_resolved = ip end)
    ip = ip_util.normalize_ip(ip)
    -- 默认有代理链；身份头尚未读到时无法区分真实访客与 CDN 回源 IP。
    -- 仅显式 REAL_IP_HEADER=off 的纯直连部署允许用公网 remote_addr 计数。
    if source == "remote" and string.lower(conf.real_ip_header) ~= "off" then
        -- 身份头存在但全部属于豁免清单：记录该清单地址被跳过，绝不回退封代理。
        local values = ip_util.split_list(header(conf.real_ip_header) or ngx.var.http_x_forwarded_for)
        local excluded = ip_util.normalize_ip(values[#values])
        if excluded and core.is_never_ban(excluded, conf) then return excluded end
        return nil, "identity_unavailable"
    end
    return ip
end

local function enqueue(event, ip, reason)
    if core.is_never_ban(ip, conf) then
        limited_log(event, ip, "skip", "never_ban")
        return
    end
    if ngx.now() < backoff_until then return end
    if tail - head + 1 >= QUEUE_LIMIT then
        limited_log("system", nil, "drop", "queue_full")
        return
    end
    tail = tail + 1
    queue[tail] = {event = event, ip = ip, reason = reason}
end

-- Redis TIME 是跨实例统一时钟；列表保存最近 threshold+1 个事件时间。
-- 每个时间戳最多入队/出队一次，不做全库扫描。LTRIM 保证阈值调整后列表仍有界。
-- 全部逻辑在一次 EVAL 中完成：计数、过期、判定、SET NX EX，不覆盖管理员长封/永久封禁。
local COUNT_AND_BAN = [[
if redis.call('EXISTS', KEYS[2]) == 1 then return {0, 0} end
local clock = redis.call('TIME')
local now = tonumber(clock[1]) * 1000 + math.floor(tonumber(clock[2]) / 1000)
local cutoff = now - tonumber(ARGV[1]) * 1000
local threshold = tonumber(ARGV[2])
redis.call('LPUSH', KEYS[1], tostring(now))
redis.call('LTRIM', KEYS[1], 0, threshold)
local count = redis.call('LLEN', KEYS[1])
while count > 0 do
    local oldest = tonumber(redis.call('LINDEX', KEYS[1], -1))
    if oldest > cutoff then break end
    redis.call('RPOP', KEYS[1])
    count = count - 1
end
redis.call('EXPIRE', KEYS[1], ARGV[1])
if count > threshold then
    local applied = redis.call('SET', KEYS[2], ARGV[4], 'EX', ARGV[3], 'NX')
    if applied then
        redis.call('DEL', KEYS[1])
        return {count, 1}
    end
end
return {count, 0}
]]

local function connect()
    local red = redis:new()
    red:set_timeouts(tonumber(os.getenv("REDIS_CONNECT_TIMEOUT_MS")) or 500,
        tonumber(os.getenv("REDIS_READ_TIMEOUT_MS")) or 500,
        tonumber(os.getenv("REDIS_READ_TIMEOUT_MS")) or 500)
    local ok, err = red:connect(os.getenv("REDIS_HOST") or "redis",
        tonumber(os.getenv("REDIS_PORT")) or 6379)
    if not ok then return nil, err end
    local password = os.getenv("REDIS_PASSWORD") or ""
    if password ~= "" then
        ok, err = red:auth(password)
        if not ok then red:close(); return nil, err end
    end
    local db = tonumber(os.getenv("REDIS_DB")) or 0
    if db ~= 0 then
        ok, err = red:select(db)
        if not ok then red:close(); return nil, err end
    end
    return red
end

local function flush(premature)
    if premature then return end
    -- 无请求的连接层错误也要定期消费；不从这里对任何 client: 地址计数。
    drain_native(nil)
    if head > tail or ngx.now() < backoff_until then return end
    local red, err = connect()
    if not red then
        limited_log("system", nil, "error", "redis_unavailable:" .. tostring(err))
        queue, head, tail = {}, 1, 0  -- 故障期间不积压/重放旧事件
        backoff_until = ngx.now() + 5
        return
    end
    local last = math.min(tail, head + BATCH_SIZE - 1)
    for _ = head, last do
        local item = queue[head]
        queue[head], head = nil, head + 1
        -- 执行器再次校验 never-ban；不信任外部传入的 IP 或日志里的对端字段。
        if not core.is_never_ban(item.ip, conf) then
            -- 与 Java RedisUtil 的 JSON String 序列化兼容，否则后台读黑名单会删除裸字符串。
            local reason = cjson.encode("auto:nginx:" .. item.event .. ":" .. item.reason)
            local result, eval_err = red:eval(COUNT_AND_BAN, 2,
                COUNTER_PREFIX .. item.event .. ":" .. item.ip, BLACKLIST_PREFIX .. item.ip,
                window, events[item.event].threshold, ban_ttl, reason)
            if not result then
                limited_log("system", nil, "error", "redis_eval_failed:" .. tostring(eval_err))
                red:close()
                queue, head, tail = {}, 1, 0
                backoff_until = ngx.now() + 5
                return
            end
            if tonumber(result[2]) == 1 then
                log(item.event, item.ip, "ban", item.reason, tonumber(result[1]))
            end
        end
    end
    red:set_keepalive(10000, 10)
    if head > tail then queue, head, tail = {}, 1, 0 end
end

function M.init_worker()
    if not active then return end
    local ok, value = pcall(core.load_conf)
    if not ok then
        active = false
        ngx.log(ngx.ERR, "attack_event configuration failed; collection disabled: ", value)
        return
    end
    conf = value
    local captured, err = errlog.set_filter_level(ngx.INFO)
    if not captured then
        active = false
        ngx.log(ngx.ERR, "attack_event capture failed; collection disabled: ", err)
        return
    end
    local flushing = false
    local started, timer_err = ngx.timer.every(0.1, function(premature)
        -- Redis cosocket 会让出执行权，慢连接期间不能启动第二个消费者。
        if flushing then return end
        flushing = true
        local success, failure = pcall(flush, premature)
        flushing = false
        if not success then limited_log("system", nil, "error", tostring(failure)) end
    end)
    if not started then
        active = false
        ngx.log(ngx.ERR, "attack_event timer failed; collection disabled: ", timer_err)
    end
end

function M.log_request()
    if not active or not conf then return end
    local status = ngx.status
    -- 不访问 URI/UA/请求头内容做特征扫描。正常请求只取状态并消费最多 128 条原生日志。
    local native = drain_native(ngx.var.connection)
    local event, reason
    if native and ((status == 400 and native.event ~= "E1")
        or (status == 400 and native.event == "E1" and ngx.var.http2 == "h2")
        or (status == 414 and native.event == "E5")) then
        event, reason = native.event, native.reason
    elseif status == 414 and events.E5.on then
        event, reason = "E5", "uri_too_long"
    elseif events.E6.on and (status == 400 or status == 404 or status == 405)
       and not native and (not ngx.var.upstream_status or ngx.var.upstream_status == "") then
        -- Java 已有恶意路径/注入检测，所有 upstream 响应排除，避免同一攻击重复建设。
        -- 被 SPA 改写成 200 的无效路径不可见，不猜测。
        event, reason = "E6", "local_invalid_path"
    end
    if not event or not events[event].on or events[event].observe then return end
    local ip, problem = request_ip()
    if problem then
        limited_log(event, nil, "observe", problem)
    elseif core.is_never_ban(ip, conf) then
        limited_log(event, ip, "skip", "never_ban")
    else
        enqueue(event, ip, reason)
    end
end

return M
