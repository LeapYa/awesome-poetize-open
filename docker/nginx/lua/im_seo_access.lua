-- 简化的IM SEO脚本 - 仅设置基本title
-- IM聊天室页面的简单fallback

local cjson = require "cjson"
local http = require "resty.http"

-- 基本的安全检查
local uri = ngx.var.uri
if uri and (string.find(uri, "%.%.") or string.len(uri) > 200) then
    ngx.log(ngx.WARN, "IM页面检测到可疑URI: " .. uri)
    return
end

-- 初始化简单上下文
ngx.ctx.title = ""
ngx.ctx.title_injected = false

-- 轻量级获取网站名称（复用主站逻辑）
local function get_site_name()
    local seo_cache = ngx.shared.seo_cache
    local cache_key = "site_name_basic"
    
    local cached_name = seo_cache:get(cache_key)
    if cached_name then
        return cached_name
    end
    
    -- 轻量级HTTP请求获取网站名称
    local httpc = http.new()
    httpc:set_timeouts(2000, 3000, 3000)
    
    local res, err = httpc:request_uri("http://poetize-java:8081/webInfo/getWebInfo", {
        method = "GET",
        headers = {
            ["Connection"] = "keep-alive",
            ["X-Internal-Service"] = "poetize-nginx-im-fallback",
            ["User-Agent"] = "nginx-lua-im-fallback/1.0"
        },
        keepalive_timeout = 30000,
        keepalive_pool = 5
    })
    
    local site_name = "POETIZE"
    
    if res and res.status == 200 and res.body then
        local ok, data = pcall(cjson.decode, res.body)
        if ok and data and data.data then
            -- 优先使用webTitle（网站标题），fallback到webName（网站名称）
            if data.data.webTitle and data.data.webTitle ~= "" then
                site_name = data.data.webTitle
            elseif data.data.webName and data.data.webName ~= "" then
                site_name = data.data.webName
            end
            seo_cache:set(cache_key, site_name, 1800) -- 缓存30分钟
        end
    end
    
    httpc:set_keepalive(30000, 5)
    return site_name
end

-- 设置IM页面的简单title
local site_name = get_site_name()
ngx.ctx.title = site_name .. "聊天室"