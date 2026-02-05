-- 简化的SEO fallback脚本 - 仅处理title标签
-- prerender已处理大部分情况，这里只作为fallback

local cjson = require "cjson"
local http = require "resty.http"

local uri = ngx.var.uri
if uri then
    -- 快速检查：如果是静态资源，直接跳过
    local static_extensions = {
        css = true, js = true, png = true, jpg = true, jpeg = true,
        gif = true, ico = true, svg = true, woff = true, woff2 = true,
        ttf = true, eot = true, map = true, pdf = true, txt = true,
        xml = true, json = true, zip = true, webp = true, avif = true
    }
    
    local ext = string.match(uri, "%.([^%.]+)$")
    if ext and static_extensions[string.lower(ext)] then
    return
end

    -- 检查API路径
    if string.sub(uri, 1, 5) == "/api/" or
       string.sub(uri, 1, 8) == "/python/" or
       string.sub(uri, 1, 6) == "/ws/im" then
        return
    end
end

-- 初始化上下文
ngx.ctx.title = ""
ngx.ctx.title_injected = false

-- 轻量级获取网站基本信息
local function get_site_name()
    local seo_cache = ngx.shared.seo_cache
    local cache_key = "site_name_basic"
    
    -- 先尝试从缓存获取
    local cached_name = seo_cache:get(cache_key)
    if cached_name then
        return cached_name
    end
    
    -- 发起轻量级HTTP请求获取网站基本信息
    local httpc = http.new()
    httpc:set_timeouts(2000, 3000, 3000) -- 更短的超时时间
    
    local res, err = httpc:request_uri("http://poetize-java:8081/webInfo/getWebInfo", {
        method = "GET",
        headers = {
            ["Connection"] = "keep-alive",
            ["X-Internal-Service"] = "poetize-nginx",
            ["User-Agent"] = "nginx-lua/1.0"
        },
        keepalive_timeout = 30000,
        keepalive_pool = 5
    })
    
    local site_name = "POETIZE" -- 默认值
    
    if res and res.status == 200 and res.body then
    local ok, data = pcall(cjson.decode, res.body)
        if ok and data and data.data then
            -- 优先使用webTitle（网站标题），fallback到webName（网站名称）
            if data.data.webTitle and data.data.webTitle ~= "" then
                site_name = data.data.webTitle
            elseif data.data.webName and data.data.webName ~= "" then
                site_name = data.data.webName
            end
            -- 缓存30分钟
            seo_cache:set(cache_key, site_name, 1800)
        end
    end
    
    httpc:set_keepalive(30000, 5)
    return site_name
end

-- 简单的title生成逻辑（使用真实网站名称）
local function generate_simple_title()
    local site_name = get_site_name()
    
    -- 从URL提取信息生成基本title
    if uri then
        if string.find(uri, "^/article/") then
            return "文章详情 - " .. site_name
        elseif string.find(uri, "^/sort/") then
            local sort_id = string.match(uri, "/sort/(%d+)")
            if sort_id then
                return "分类 - " .. site_name
            elseif uri == "/sort" then
                return "文章分类 - " .. site_name
            end
        elseif uri == "/" then
            return site_name
        elseif uri == "/favorite" then
            return "百宝箱 - " .. site_name
        elseif uri == "/about" then
            return "关于我们 - " .. site_name
        elseif uri == "/message" then
            return "留言板 - " .. site_name
        elseif uri == "/weiYan" then
            return "微言 - " .. site_name
        elseif uri == "/love" then
            return "恋爱记录 - " .. site_name
        elseif uri == "/travel" then
            return "旅行日记 - " .. site_name
        elseif uri == "/privacy" then
            return "隐私政策 - " .. site_name
        elseif uri == "/user" then
            return "登录 - " .. site_name
        elseif uri == "/letter" then
            return "信件 - " .. site_name
        elseif uri == "/verify" then
            return "身份验证 - " .. site_name
        elseif uri == "/oauth-callback" then
            return "登录验证中 - " .. site_name
        end
    end
    
    return site_name
end

-- 设置真实的title（仅在prerender失效时使用）
ngx.ctx.title = generate_simple_title()