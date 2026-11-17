package com.ld.poetry.config;

import com.ld.poetry.service.CacheService;
import com.ld.poetry.service.provider.Ip2RegionProvider;
import com.ld.poetry.utils.RedisUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SecurityFilter 扩展封禁规则（UA/CIDR/Region）匹配逻辑单元测试。
 *
 * <p>匹配方法（isCidrBanned/isRegionBanned/isUaBannedByAdmin）均为 private，测试策略为
 * 通过 {@code doFilter} 行为验证匹配结果：命中封禁返回 403 + 特定关键词，未命中则放行至 FilterChain。
 */
class SecurityFilterBanRuleTest {

    private SecurityFilter filter;
    private RedisUtil redisUtil;
    private CacheService cacheService;
    private Ip2RegionProvider ip2RegionProvider;

    @BeforeEach
    void setUp() {
        redisUtil = mock(RedisUtil.class);
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        when(redisUtil.incr(anyString(), anyLong())).thenReturn(0L);

        cacheService = mock(CacheService.class);
        when(cacheService.loadAllBanRules()).thenReturn(null);

        ip2RegionProvider = mock(Ip2RegionProvider.class);

        filter = new SecurityFilter();
        ReflectionTestUtils.setField(filter, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(filter, "cacheService", cacheService);
        ReflectionTestUtils.setField(filter, "ip2RegionProvider", ip2RegionProvider);
        // 设置最近刷新时间，避免 refreshBanRulesIfStale 触发 Redis 加载覆盖手动注入的规则
        ReflectionTestUtils.setField(filter, "lastRefreshAt", System.currentTimeMillis());
    }

    // ================================ UA 规则匹配 ================================

    @Test
    @DisplayName("UA contains 匹配：规则 contains 'semrush'，请求 UA 含 'SemrushBot'，应返回 403")
    void uaContainsMatch_blocksRequest() throws Exception {
        setUaRules(Map.of("value", "semrush", "matchMode", "contains"));

        MockHttpServletRequest request = newRequest("1.2.3.4", "Mozilla/5.0 (compatible; SemrushBot/7~bl; +http://www.semrush.com/bot.html)");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("UA Blacklisted"));
    }

    @Test
    @DisplayName("UA equals 匹配：规则 equals 'curl/8.0.1'，请求 UA 恰好 'curl/8.0.1'，应返回 403")
    void uaEqualsMatch_blocksRequest() throws Exception {
        setUaRules(Map.of("value", "curl/8.0.1", "matchMode", "equals"));

        MockHttpServletRequest request = newRequest("1.2.3.4", "curl/8.0.1");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("UA Blacklisted"));
    }

    @Test
    @DisplayName("UA 大小写不敏感：规则 'SemRush' contains，请求 UA 'semrushbot'，应返回 403")
    void uaCaseInsensitive_blocksRequest() throws Exception {
        setUaRules(Map.of("value", "SemRush", "matchMode", "contains"));

        MockHttpServletRequest request = newRequest("1.2.3.4", "semrushbot/2.1");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("UA Blacklisted"));
    }

    @Test
    @DisplayName("UA 不匹配：规则 'semrush'，请求 UA 'Mozilla/5.0'，应放行")
    void uaNotMatch_passesThrough() throws Exception {
        setUaRules(Map.of("value", "semrush", "matchMode", "contains"));

        MockHttpServletRequest request = newRequest("1.2.3.4", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("空 UA 跳过检查：不设 UA 头，有 UA 规则，应放行")
    void emptyUa_skipsCheck() throws Exception {
        setUaRules(Map.of("value", "semrush", "matchMode", "contains"));

        MockHttpServletRequest request = newRequest("1.2.3.4", null);
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // ================================ CIDR 规则匹配 ================================

    @Test
    @DisplayName("CIDR 匹配：规则 '192.168.1.0/24'，客户端 IP '192.168.1.55'，应返回 403")
    void cidrMatch_blocksRequest() throws Exception {
        setCidrRules(Map.of("value", "192.168.1.0/24"));

        MockHttpServletRequest request = newRequest("192.168.1.55", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("CIDR Blacklisted"));
    }

    @Test
    @DisplayName("CIDR 不匹配：规则 '192.168.1.0/24'，客户端 IP '192.168.2.1'，应放行")
    void cidrNotMatch_passesThrough() throws Exception {
        setCidrRules(Map.of("value", "192.168.1.0/24"));

        MockHttpServletRequest request = newRequest("192.168.2.1", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("CIDR IPv6 匹配：规则 '2001:db8::/32'，客户端 IP '2001:db8::1'，应返回 403")
    void cidrIpv6Match_blocksRequest() throws Exception {
        setCidrRules(Map.of("value", "2001:db8::/32"));

        MockHttpServletRequest request = newRequest("2001:db8::1", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("CIDR Blacklisted"));
    }

    @Test
    @DisplayName("CIDR IPv6 不匹配：规则 '2001:db8::/32'，客户端 IP '2001:db9::1'，应放行")
    void cidrIpv6NotMatch_passesThrough() throws Exception {
        setCidrRules(Map.of("value", "2001:db8::/32"));

        MockHttpServletRequest request = newRequest("2001:db9::1", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // ================================ Region 规则匹配 ================================

    @Test
    @DisplayName("Region country 匹配：规则 country '美国'，ip2region 返回 ['美国','加利福尼亚','洛杉矶']，应返回 403")
    void regionCountryMatch_blocksRequest() throws Exception {
        setRegionRules(Map.of("value", "美国", "regionType", "country"));
        when(ip2RegionProvider.resolveLocationDetail(anyString()))
                .thenReturn(new String[]{"美国", "加利福尼亚", "洛杉矶"});

        MockHttpServletRequest request = newRequest("8.8.8.8", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Region Blacklisted"));
    }

    @Test
    @DisplayName("Region province 匹配：规则 province '广东'，ip2region 返回 ['中国','广东','深圳']，应返回 403")
    void regionProvinceMatch_blocksRequest() throws Exception {
        setRegionRules(Map.of("value", "广东", "regionType", "province"));
        when(ip2RegionProvider.resolveLocationDetail(anyString()))
                .thenReturn(new String[]{"中国", "广东", "深圳"});

        MockHttpServletRequest request = newRequest("120.239.141.213", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Region Blacklisted"));
    }

    @Test
    @DisplayName("Region 前缀容错：规则 '广东'，ip2region 返回省份 '广东省'，应返回 403")
    void regionPrefixTolerance_blocksRequest() throws Exception {
        setRegionRules(Map.of("value", "广东", "regionType", "province"));
        when(ip2RegionProvider.resolveLocationDetail(anyString()))
                .thenReturn(new String[]{"中国", "广东省", "深圳"});

        MockHttpServletRequest request = newRequest("120.239.141.213", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Region Blacklisted"));
    }

    @Test
    @DisplayName("Region 解析失败：有 region 规则，ip2region 返回 [null,null,null]，应放行（不误伤）")
    void regionResolveFail_passesThrough() throws Exception {
        setRegionRules(Map.of("value", "广东", "regionType", "province"));
        when(ip2RegionProvider.resolveLocationDetail(anyString()))
                .thenReturn(new String[]{null, null, null});

        MockHttpServletRequest request = newRequest("1.2.3.4", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // ================================ 空规则 ================================

    @Test
    @DisplayName("三类规则都为空，应放行")
    void emptyRules_passesThrough() throws Exception {
        // 规则列表默认为 List.of()（空），无需额外设置

        MockHttpServletRequest request = newRequest("1.2.3.4", "Mozilla/5.0");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // ================================ 编码绕过 ================================

    @Test
    @DisplayName("URL 编码的 /.env 应拦截：/%2eenv 解码后命中精确黑名单")
    void encodedEnvPath_blocksRequest() throws Exception {
        assertBlockedAsMalicious("/%2eenv");
    }

    @Test
    @DisplayName("多重 URL 编码的 /.env 应拦截：/%252eenv 需多轮解码才能识别")
    void doubleEncodedEnvPath_blocksRequest() throws Exception {
        assertBlockedAsMalicious("/%252eenv");
    }

    @Test
    @DisplayName("编码隐藏文件变体应拦截：/%2eenv.production 解码后命中前缀规则")
    void encodedEnvVariant_blocksRequest() throws Exception {
        assertBlockedAsMalicious("/%2eenv.production");
    }

    @Test
    @DisplayName("编码斜杠与隐藏文件应拦截：/%2f.env 归一化后为 /.env")
    void encodedSlashEnvPath_blocksRequest() throws Exception {
        assertBlockedAsMalicious("/%2f.env");
    }

    @Test
    @DisplayName("path params 绕过应拦截：/.env;x 按 /.env 匹配")
    void pathParamEnvPath_blocksRequest() throws Exception {
        assertBlockedAsMalicious("/.env;x");
    }

    @Test
    @DisplayName("NUL 截断绕过应拦截：/.env%00.jpg 截断后按 /.env 匹配")
    void nulSuffixEnvPath_blocksRequest() throws Exception {
        assertBlockedAsMalicious("/.env%00.jpg");
    }

    @Test
    @DisplayName("畸形百分号编码应拦截：/%zz 走 malformedUri 分支")
    void malformedPercentEncoding_blocksRequest() throws Exception {
        assertBlockedAsMalicious("/%zz");
    }

    @Test
    @DisplayName("正常含 env 字样的文章路径不应误伤：/article/how-to-use-dotenv 放行")
    void normalEnvLikePath_passesThrough() throws Exception {
        MockHttpServletRequest request = newRequest("1.2.3.4", "Mozilla/5.0");
        request.setRequestURI("/article/how-to-use-dotenv");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    private void assertBlockedAsMalicious(String uri) throws Exception {
        MockHttpServletRequest request = newRequest("1.2.3.4", "curl/8.0.1");
        request.setRequestURI(uri);
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(404, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    // ================================ 脚本/命令行客户端 UA ================================

    @Test
    @DisplayName("curl 抓首页应 403，且不写攻击计数、不写 IP 封禁")
    void httpClientUa_homePage_blocksWithoutBan() throws Exception {
        MockHttpServletRequest request = newRequest("1.2.3.4", "curl/8.4.0");
        request.setMethod("GET");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("HTTP Client Blocked"));
        verify(chain, never()).doFilter(request, response);
        verify(redisUtil, never()).incr(anyString(), anyLong());
        verify(cacheService, never()).set(anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("内置脚本客户端 UA 抓文章页应 403（前缀匹配，大小写不敏感）")
    void httpClientUa_articlePage_blocksAllBuiltinPrefixes() throws Exception {
        String[] uaList = {
                "curl/8.4.0", "Wget/1.21.2", "python-requests/2.31.0", "Python-urllib/3.11",
                "python-urllib3/2.0.7", "Go-http-client/1.1", "Scrapy/2.11.0 (+https://scrapy.org)",
                "libwww-perl/6.67", "http.rb/5.1.0", "Java/17.0.1", "Java-http-client/17.0.1"};
        for (String ua : uaList) {
            MockHttpServletRequest request = newRequest("1.2.3.4", ua);
            request.setMethod("GET");
            request.setRequestURI("/article/123");
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(403, response.getStatus(), "UA 应被拦截: " + ua);
            verify(chain, never()).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("curl 访问接口/CLI/健康检查/回调/资源/订阅源均放行")
    void httpClientUa_exemptPaths_passThrough() throws Exception {
        String[] paths = {
                "/api/article/1", "/api/api/article/createAsync", "/internal/monitor/health",
                "/admin", "/user/getUserInfo", "/login/github", "/callback/github",
                "/ws/im", "/media/41f8a21f9be24994b4220cf23bc482e7",
                "/static/articleFile/2025/01/report.pdf", "/prerender/home/index.html", "/seo/robots.txt",
                "/assets/index-abc123.js", "/sitemap.xml", "/feed", "/rss.xml", "/robots.txt",
                "/manifest.json", "/favicon.ico", "/.well-known/acme-challenge/token"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", "curl/8.4.0");
            request.setMethod("GET");
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            assertEquals(200, response.getStatus(), "路径应放行: " + path);
        }
    }

    @Test
    @DisplayName("HEAD 首页与 POST 支付回调即使用 curl UA 也放行")
    void httpClientUa_nonGetMethods_passThrough() throws Exception {
        MockHttpServletRequest head = newRequest("1.2.3.4", "curl/8.4.0");
        head.setMethod("HEAD");
        MockHttpServletResponse headResponse = newResponse();
        FilterChain headChain = mock(FilterChain.class);
        filter.doFilter(head, headResponse, headChain);
        verify(headChain).doFilter(head, headResponse);

        MockHttpServletRequest post = newRequest("1.2.3.4", "curl/8.4.0");
        post.setMethod("POST");
        post.setRequestURI("/payment/webhook/alipay");
        MockHttpServletResponse postResponse = newResponse();
        FilterChain postChain = mock(FilterChain.class);
        filter.doFilter(post, postResponse, postChain);
        verify(postChain).doFilter(post, postResponse);
    }

    @Test
    @DisplayName("浏览器与应用侧客户端 UA 放行：Apache-HttpClient 内嵌 (Java/...) 不得误伤")
    void httpClientUa_browserAndAppClients_passThrough() throws Exception {
        String[] uaList = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
                "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
                "Apache-HttpClient/4.5.13 (Java/1.8.0_292)", "okhttp/4.9.0", "axios/1.6.0",
                "node-fetch/1.0", "python-httpx/0.25.0", "poetize-java/1.0.0",
                "Poetize-Deploy-Check/1.0", null};
        for (String ua : uaList) {
            MockHttpServletRequest request = newRequest("1.2.3.4", ua);
            request.setMethod("GET");
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            assertEquals(200, response.getStatus(), "UA 应放行: " + ua);
        }
    }

    @Test
    @DisplayName("管理员禁用内置 curl 硬名单后，curl 抓首页放行")
    void httpClientUa_disabledByAdmin_passThrough() throws Exception {
        ReflectionTestUtils.setField(filter, "disabledHttpClientUa", Set.of("curl"));

        MockHttpServletRequest request = newRequest("1.2.3.4", "curl/8.4.0");
        request.setMethod("GET");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("编码绕过仍命中：curl 抓 /%61rticle/123 归一化后按内容页拦截")
    void httpClientUa_encodedContentPath_blocksRequest() throws Exception {
        MockHttpServletRequest request = newRequest("1.2.3.4", "curl/8.4.0");
        request.setMethod("GET");
        request.setRequestURI("/%61rticle/123");
        MockHttpServletResponse response = newResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    // ================================ 恶意路径与敏感后缀 ================================

    @Test
    @DisplayName("新增扫描路径黑名单命中返回 404，含同族前缀变体")
    void maliciousPaths_scannerProbes_return404() throws Exception {
        String[] paths = {
                "/actuator", "/actuator/env", "/actuator/heapdump", "/actuator/configprops",
                "/actuator/mappings", "/actuator/loggers", "/actuator/beans", "/actuator/health",
                "/graphql", "/v1/graphql", "/api/graphql", "/graphql/console",
                "/swagger", "/swagger-ui", "/swagger-ui/index.html", "/swagger-resources",
                "/api-docs", "/v2/api-docs", "/v3/api-docs", "/v3/api-docs/swagger-config",
                "/openapi.json", "/.well-known/jwks.json", "/webhooks",
                "/_vti_pvt", "/_vti_pvt/service.cnf", "/server-status", "/server-info",
                "/phpinfo.php", "/info.php"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(404, response.getStatus(), "路径应拦截: " + path);
            verify(chain, never()).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("大小写与编码绕过仍命中新增黑名单")
    void maliciousPaths_caseAndEncodingBypass_return404() throws Exception {
        String[] paths = {"/Actuator/ENV", "/%61ctuator/heapdump", "//actuator/env",
                "/actuator;/env", "/SWAGGER-UI/index.html", "/%2eenv"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(404, response.getStatus(), "路径应拦截: " + path);
            verify(chain, never()).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("新增敏感后缀命中返回 404")
    void sensitiveExtensions_keyPemMapZip_return404() throws Exception {
        String[] paths = {"/server.key", "/id_rsa.pem", "/assets/index-abc123.js.map",
                "/backup.zip", "/dump.tar.gz", "/database.sql", "/backup.bak"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(404, response.getStatus(), "后缀应拦截: " + path);
            verify(chain, never()).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("真实路由不得误封：/admin、/login、/settings 与 ACME/IndexNow/健康检查")
    void realRoutes_mustNotBeBlocked() throws Exception {
        String[] paths = {"/admin", "/admin/", "/admin/map/world-cn.json", "/login", "/settings",
                "/.well-known/acme-challenge/token", "/internal/monitor/health",
                "/abcdefgh12345678.txt", "/fontSubset/download", "/sitemap.xml", "/manifest.json"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus(), "真实路由应放行: " + path);
            verify(chain).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("用户上传附件不按后缀封禁：nginx 会把存在的上传文件转发到 Java")
    void uploadedAttachments_sensitiveExtensions_passThrough() throws Exception {
        String[] paths = {"/static/articleFile/2025/01/backup.zip", "/static/articleFile/config/b.sql",
                "/static/articleFile/demo/server.key", "/static/articleFile/demo/cert.pem",
                "/static/articleFile/demo/app.js.map", "/static/articleFile/demo/dump.tar.gz",
                "/static/articleFile/demo/pack.tgz", "/static/articleFile/demo/app.jar",
                "/static/articleFile/2025/01/report.pdf"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus(), "附件应放行: " + path);
            verify(chain).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("尾点与 /. 段变体仍命中：换尾巴不能绕过精确集合与后缀检测")
    void maliciousPaths_trailingDotVariants_return404() throws Exception {
        String[] paths = {"/backup.zip.", "/backup.zip/.", "/backup.zip/./", "/server.key/.",
                "/dump.sql.", "/dump.tar.gz/.", "/app.js.map.", "/id_rsa.pem/.",
                "/.well-known/jwks.json.", "/.well-known/jwks.json/.",
                "/webhooks.", "/webhooks/.", "/server-status/.", "/server-info.",
                "/openapi.json.", "/v1/graphql/.", "/api/graphql.", "/actuator/env/."};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(404, response.getStatus(), "尾点变体应拦截: " + path);
            verify(chain, never()).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("归档后缀与 nginx 名单对齐：tgz/tar/rar/7z/bz2/jar/war 均拦截")
    void sensitiveExtensions_archiveSuffixes_return404() throws Exception {
        String[] paths = {"/backup.tgz", "/backup.tar", "/backup.rar", "/backup.7z",
                "/backup.bz2", "/backup.jar", "/backup.war"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(404, response.getStatus(), "归档后缀应拦截: " + path);
            verify(chain, never()).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("预压缩静态产物必须放行：.gz 不得进入敏感后缀名单")
    void precompressedAssets_mustNotBeBlocked() throws Exception {
        String[] paths = {"/assets/index-abc123.js.gz", "/assets/app-def456.css.gz",
                "/admin/map/world-cn.json.gz", "/assets/app.js.map.gz", "/sitemap.xml.gz"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus(), "预压缩资源应放行: " + path);
            verify(chain).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("剥尾点不得吞掉 .. ：路径遍历特征仍被拦截")
    void trailingDotStrip_mustPreserveTraversalDetection() throws Exception {
        String[] paths = {"/foo/..", "/static/..", "/backup.zip..", "/static/../.env"};
        for (String path : paths) {
            MockHttpServletRequest request = newRequest("1.2.3.4", BROWSER_UA);
            request.setRequestURI(path);
            MockHttpServletResponse response = newResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(404, response.getStatus(), "遍历特征应拦截: " + path);
            verify(chain, never()).doFilter(request, response);
        }
    }

    // ================================ 辅助方法 ================================

    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    @SuppressWarnings("unchecked")
    private void setUaRules(Map<String, Object>... rules) {
        ReflectionTestUtils.setField(filter, "uaRules", Arrays.asList(rules));
    }

    @SuppressWarnings("unchecked")
    private void setCidrRules(Map<String, Object>... rules) {
        ReflectionTestUtils.setField(filter, "cidrRules", Arrays.asList(rules));
    }

    @SuppressWarnings("unchecked")
    private void setRegionRules(Map<String, Object>... rules) {
        ReflectionTestUtils.setField(filter, "regionRules", Arrays.asList(rules));
    }

    private MockHttpServletRequest newRequest(String remoteAddr, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/");
        request.setRemoteAddr(remoteAddr);
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        return request;
    }

    private MockHttpServletResponse newResponse() {
        return new MockHttpServletResponse();
    }
}
