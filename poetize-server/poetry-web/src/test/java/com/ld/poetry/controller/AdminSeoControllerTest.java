package com.ld.poetry.controller;

import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.service.SeoConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeoControllerTest {

    @Mock
    private SeoConfigService seoConfigService;

    private AdminSeoController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminSeoController();
        ReflectionTestUtils.setField(controller, "seoConfigService", seoConfigService);
    }

    @Test
    void analyzeSite_shouldUseUpdatedRealtimePushAndVerificationSuggestions() {
        Map<String, Object> config = createBaselineSeoConfig();
        config.put("generate_sitemap", false);
        config.put("baidu_push_enabled", true);
        config.put("baidu_push_token", "");
        config.put("bing_push_enabled", true);
        config.put("bing_api_key", "");
        config.put("google_site_verification", "");
        config.put("bing_site_verification", "");

        when(seoConfigService.getSeoConfigAsJson()).thenReturn(config);

        PoetryResult<Map<String, Object>> result = controller.analyzeSite();

        assertTrue(result.isSuccess());
        List<Map<String, Object>> suggestions = getSuggestions(result);

        assertContainsSuggestion(suggestions, "Sitemap 生成功能未启用");
        assertContainsSuggestion(suggestions, "百度推送已启用，但 Token 未配置");
        assertContainsSuggestion(suggestions, "Bing 推送已启用，但 IndexNow Key 未配置");
        assertContainsSuggestion(suggestions, "Google 站点验证未设置");
        assertContainsSuggestion(suggestions, "Bing 站点验证未设置");
        assertTrue(suggestions.stream().noneMatch(item -> String.valueOf(item.get("message")).contains("Google索引")));
    }

    @Test
    void analyzeSite_shouldRecommendSupportedRealtimeChannelsWhenNoneEnabled() {
        Map<String, Object> config = createBaselineSeoConfig();
        config.put("baidu_push_enabled", false);
        config.put("bing_push_enabled", false);
        config.put("yandex_push_enabled", false);
        config.put("sogou_push_enabled", false);
        config.put("shenma_push_enabled", false);

        when(seoConfigService.getSeoConfigAsJson()).thenReturn(config);

        PoetryResult<Map<String, Object>> result = controller.analyzeSite();

        assertTrue(result.isSuccess());
        List<Map<String, Object>> suggestions = getSuggestions(result);
        assertContainsSuggestion(suggestions, "当前未启用任何实时推送渠道，建议至少启用百度推送或 Bing(IndexNow)");

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.getData().get("issue_summary");
        assertEquals(1L, summary.get("warning_count"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSuggestions(PoetryResult<Map<String, Object>> result) {
        return (List<Map<String, Object>>) result.getData().get("suggestions");
    }

    private void assertContainsSuggestion(List<Map<String, Object>> suggestions, String expectedSnippet) {
        assertTrue(
                suggestions.stream().anyMatch(item -> String.valueOf(item.get("message")).contains(expectedSnippet)),
                () -> "未找到建议: " + expectedSnippet);
    }

    private Map<String, Object> createBaselineSeoConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enable", true);
        config.put("site_description", "这是一个用于测试 SEO 分析的站点描述，长度已经刻意拉长，用来覆盖当前规则对描述长度的要求，并确保不会因为摘要过短而触发额外的警告。");
        config.put("site_keywords", "博客,SEO,测试");
        config.put("default_author", "poetize");
        config.put("og_image", "https://example.com/og.png");
        config.put("site_logo", "https://example.com/logo.png");
        config.put("site_icon", "https://example.com/favicon.ico");
        config.put("apple_touch_icon", "");
        config.put("site_icon_192", "");
        config.put("site_icon_512", "");
        config.put("auto_generate_meta_tags", true);
        config.put("generate_sitemap", true);
        config.put("robots_txt", "User-agent: *\nAllow: /\nDisallow: /admin/\nSitemap: /sitemap.xml");
        config.put("baidu_push_enabled", false);
        config.put("baidu_push_token", "");
        config.put("bing_push_enabled", false);
        config.put("bing_api_key", "");
        config.put("yandex_push_enabled", false);
        config.put("yandex_api_key", "");
        config.put("sogou_push_enabled", false);
        config.put("sogou_push_token", "");
        config.put("shenma_push_enabled", false);
        config.put("shenma_token", "");
        config.put("baidu_site_verification", "baidu-verify");
        config.put("google_site_verification", "google-verify");
        config.put("bing_site_verification", "bing-verify");
        return config;
    }
}
