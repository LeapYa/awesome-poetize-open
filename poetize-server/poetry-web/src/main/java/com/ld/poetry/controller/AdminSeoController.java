package com.ld.poetry.controller;

import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.service.SeoConfigService;
import com.ld.poetry.service.SeoMetaService;
import com.ld.poetry.service.SeoStaticService;
import com.ld.poetry.service.SeoImageService;
import com.ld.poetry.service.SearchEnginePushService;
import com.ld.poetry.service.SitemapService;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.utils.PrerenderClient;
import com.ld.poetry.utils.security.FileSecurityValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * SEO管理功能控制器
 * 提供管理员专用的SEO配置和管理功能
 * </p>
 *
 * @author sara
 * @since 2024-12-23
 */
@RestController
@RequestMapping("/admin/seo")
@Slf4j
public class AdminSeoController {

    @Autowired
    private SeoConfigService seoConfigService;

    @Autowired
    private SeoMetaService seoMetaService;

    @Autowired
    private SeoStaticService seoStaticService;

    @Autowired
    private SeoImageService seoImageService;

    @Autowired
    private SearchEnginePushService searchEnginePushService;

    @Autowired
    private SitemapService sitemapService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PrerenderClient prerenderClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private com.ld.poetry.service.RobotsService robotsService;

    @Autowired
    private com.ld.poetry.config.PrerenderStartupRunner prerenderStartupRunner;

    @Autowired
    private FileSecurityValidator fileSecurityValidator;

    /**
     * 清除nginx SEO缓存
     * 在SEO配置更新后调用，确保nginx不使用旧的缓存数据作为fallback
     */
    private void clearNginxSeoCache() {
        try {
            String nginxUrl = "http://nginx";
            String clearCacheUrl = nginxUrl + "/flush_seo_cache";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service", "poetize-java");
            headers.set("User-Agent", "poetize-java/1.0.0");

            HttpEntity<?> request = new HttpEntity<>(headers);

            restTemplate.exchange(clearCacheUrl, HttpMethod.GET, request, String.class);
        } catch (Exception e) {
            log.warn("清除nginx SEO缓存失败: {}", e.getMessage());
            // 不抛出异常，避免影响主流程
        }
    }

    // ========== 配置管理API ==========

    /**
     * 检测网站URL
     */
    @GetMapping("/detectSiteUrl")
    public PoetryResult<Map<String, Object>> detectSiteUrl(HttpServletRequest request) {
        try {
            Map<String, Object> result = seoMetaService.detectSiteUrl(request);
            return PoetryResult.success(result);
        } catch (Exception e) {
            log.error("检测网站URL失败", e);
            return PoetryResult.fail("检测网站URL失败");
        }
    }

    /**
     * 获取SEO配置（管理员权限）
     */
    @GetMapping("/getSeoConfig")
    @LoginCheck(1)
    public PoetryResult<Map<String, Object>> getSeoConfig(HttpServletRequest request) {
        try {
            Map<String, Object> config = seoConfigService.getSeoConfigAsJson();

            return PoetryResult.success(config);
        } catch (Exception e) {
            log.error("获取SEO配置失败", e);
            return PoetryResult.fail("获取SEO配置失败");
        }
    }

    /**
     * 更新SEO配置（自动清理缓存并触发预渲染）
     */
    @PostMapping("/updateSeoConfig")
    @LoginCheck(1)
    public PoetryResult<Boolean> updateSeoConfig(@RequestBody Map<String, Object> configData) {
        try {
            boolean success = seoConfigService.updateSeoConfigFromJson(configData);
            if (success) {
                // SEO配置更新时，清除缓存、重新生成sitemap并推送、重新渲染页面
                try {
                    // 1. 重新生成sitemap并推送到搜索引擎（SEO配置变更影响所有URL）
                    if (sitemapService != null) {
                        sitemapService.updateSitemapAndPush("SEO配置更新");
                    }

                    // 2. 清除nginx SEO缓存
                    clearNginxSeoCache();

                    // 3. 异步触发预渲染，避免阻塞主流程，并确保缓存数据已完全生效
                    CompletableFuture.runAsync(() -> {
                        try {
                            // 等待2秒确保缓存完全生效并可被预渲染服务读取
                            Thread.sleep(2000);
                            prerenderStartupRunner.executeFullPrerender();
                        } catch (Exception e) {
                            log.warn("异步预渲染失败", e);
                        }
                    });

                } catch (Exception e) {
                    // 预渲染失败不影响主流程，只记录日志
                    log.warn("缓存清除或预渲染失败", e);
                }

                return PoetryResult.success(true);
            } else {
                return PoetryResult.fail("SEO配置更新失败");
            }
        } catch (Exception e) {
            log.error("更新SEO配置失败", e);
            return PoetryResult.fail("更新SEO配置失败");
        }
    }

    /**
     * 更新SEO启用状态（自动清理缓存）
     */
    @PostMapping("/updateEnableStatus")
    @LoginCheck(1)
    public PoetryResult<Boolean> updateEnableStatus(@RequestBody Map<String, Object> data) {
        try {
            Object enableObj = data.get("enable");
            boolean enable = enableObj instanceof Boolean ? (Boolean) enableObj
                    : Boolean.parseBoolean(enableObj.toString());

            Map<String, Object> config = seoConfigService.getSeoConfigAsJson();
            config.put("enable", enable);

            boolean success = seoConfigService.updateSeoConfigFromJson(config);
            if (success) {
                // 自动清理相关缓存
                clearSeoCache();
                return PoetryResult.success(true);
            } else {
                return PoetryResult.fail("SEO状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新SEO状态失败", e);
            return PoetryResult.fail("更新SEO状态失败");
        }
    }

    // ========== 缓存管理API ==========

    /**
     * 清理SEO缓存（管理员功能）
     */
    @PostMapping("/clearCache")
    @LoginCheck(1)
    public PoetryResult<Boolean> clearCache() {
        try {
            clearSeoCache();
            return PoetryResult.success(true);
        } catch (Exception e) {
            log.error("清理SEO缓存失败", e);
            return PoetryResult.fail("清理SEO缓存失败");
        }
    }

    /**
     * 清理特定文章缓存
     */
    @PostMapping("/clearArticleCache")
    @LoginCheck(1)
    public PoetryResult<Boolean> clearArticleCache(@RequestBody Map<String, Object> data) {
        try {
            Object articleIdObj = data.get("articleId");
            if (articleIdObj != null) {
                String cacheKey = "seo:article:" + articleIdObj;
                cacheService.deleteKey(cacheKey);
            }
            return PoetryResult.success(true);
        } catch (Exception e) {
            log.error("清理文章SEO缓存失败", e);
            return PoetryResult.fail("清理文章SEO缓存失败");
        }
    }

    /**
     * 批量清理文章缓存
     */
    @PostMapping("/clearArticlesCache")
    @LoginCheck(1)
    public PoetryResult<Boolean> clearArticlesCache(@RequestBody Map<String, Object> data) {
        try {
            Object articleIdsObj = data.get("articleIds");
            if (articleIdsObj instanceof Iterable) {
                for (Object articleId : (Iterable<?>) articleIdsObj) {
                    String cacheKey = "seo:article:" + articleId;
                    cacheService.deleteKey(cacheKey);
                }
            }
            return PoetryResult.success(true);
        } catch (Exception e) {
            log.error("批量清理文章SEO缓存失败", e);
            return PoetryResult.fail("批量清理文章SEO缓存失败");
        }
    }

    // ========== SEO分析API ==========

    /**
     * 分析网站SEO配置
     */
    @GetMapping("/analyzeSite")
    @LoginCheck(1)
    public PoetryResult<Map<String, Object>> analyzeSite() {
        try {
            Map<String, Object> seoConfig = seoConfigService.getSeoConfigAsJson();

            // 检查SEO是否启用
            if (!Boolean.TRUE.equals(seoConfig.get("enable"))) {
                return PoetryResult.fail(403, "SEO功能未启用");
            }

            Map<String, Object> analysis = performSeoAnalysis(seoConfig);
            return PoetryResult.success(analysis);
        } catch (Exception e) {
            log.error("SEO分析失败", e);
            return PoetryResult.fail("SEO分析失败");
        }
    }

    // ========== 图像处理API ==========

    /**
     * 处理单个图片
     */
    @PostMapping("/processImage")
    @LoginCheck(1)
    public PoetryResult<Map<String, Object>> processImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "target_type", defaultValue = "logo") String targetType,
            @RequestParam(value = "preferred_format", required = false) String preferredFormat) {
        try {
            // 检查文件大小是否超过Integer.MAX_VALUE，防止溢出
            long fileSize = imageFile.getSize();
            if (fileSize > Integer.MAX_VALUE) {
                log.error("文件大小超过系统限制: {} bytes, 最大允许: {} bytes", fileSize, Integer.MAX_VALUE);
                return PoetryResult.fail("文件大小超过系统限制(" + (Integer.MAX_VALUE / 1024 / 1024) + "MB)，请上传较小的文件");
            }

            // 验证文件安全性
            FileSecurityValidator.ValidationResult validationResult = fileSecurityValidator.validateFile(imageFile,
                    imageFile.getOriginalFilename(), imageFile.getContentType());

            if (!validationResult.isSuccess()) {
                log.warn("文件安全验证失败: {}", validationResult.getMessage());
                return PoetryResult.fail("文件验证失败: " + validationResult.getMessage());
            }

            Map<String, Object> result = seoImageService.processImage(imageFile, targetType, preferredFormat);

            if ((Integer) result.get("code") != 200) {
                return PoetryResult.fail(result.get("message").toString());
            }

            return PoetryResult.success((Map<String, Object>) result.get("data"));
        } catch (Exception e) {
            log.error("图片处理失败", e);
            return PoetryResult.fail("图片处理失败");
        }
    }

    /**
     * 批量处理图标
     */
    @PostMapping("/batchProcessIcons")
    @LoginCheck(1)
    public PoetryResult<Map<String, Object>> batchProcessIcons(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "iconTypes", required = false) java.util.List<String> iconTypes) {
        try {
            // 检查文件大小是否超过Integer.MAX_VALUE，防止溢出
            long fileSize = imageFile.getSize();
            if (fileSize > Integer.MAX_VALUE) {
                log.error("文件大小超过系统限制: {} bytes, 最大允许: {} bytes", fileSize, Integer.MAX_VALUE);
                return PoetryResult.fail("文件大小超过系统限制(" + (Integer.MAX_VALUE / 1024 / 1024) + "MB)，请上传较小的文件");
            }

            // 验证文件安全性
            FileSecurityValidator.ValidationResult validationResult = fileSecurityValidator.validateFile(imageFile,
                    imageFile.getOriginalFilename(), imageFile.getContentType());

            if (!validationResult.isSuccess()) {
                log.warn("文件安全验证失败: {}", validationResult.getMessage());
                return PoetryResult.fail("文件验证失败: " + validationResult.getMessage());
            }

            Map<String, Object> result = seoImageService.batchProcessIcons(imageFile, iconTypes);

            if ((Integer) result.get("code") != 200) {
                return PoetryResult.fail(result.get("message").toString());
            }

            return PoetryResult.success((Map<String, Object>) result.get("data"));
        } catch (Exception e) {
            log.error("批量图标处理失败", e);
            return PoetryResult.fail("批量图标处理失败");
        }
    }

    /**
     * 获取图片信息
     */
    @PostMapping("/getImageInfo")
    @LoginCheck(1)
    public PoetryResult<Map<String, Object>> getImageInfo(@RequestParam("image") MultipartFile imageFile) {
        try {
            // 检查文件大小是否超过Integer.MAX_VALUE，防止溢出
            long fileSize = imageFile.getSize();
            if (fileSize > Integer.MAX_VALUE) {
                log.error("文件大小超过系统限制: {} bytes, 最大允许: {} bytes", fileSize, Integer.MAX_VALUE);
                return PoetryResult.fail("文件大小超过系统限制(" + (Integer.MAX_VALUE / 1024 / 1024) + "MB)，请上传较小的文件");
            }

            // 验证文件安全性
            FileSecurityValidator.ValidationResult validationResult = fileSecurityValidator.validateFile(imageFile,
                    imageFile.getOriginalFilename(), imageFile.getContentType());

            if (!validationResult.isSuccess()) {
                log.warn("文件安全验证失败: {}", validationResult.getMessage());
                return PoetryResult.fail("文件验证失败: " + validationResult.getMessage());
            }

            Map<String, Object> result = seoImageService.getImageInfo(imageFile);

            if ((Integer) result.get("code") != 200) {
                return PoetryResult.fail(result.get("message").toString());
            }

            return PoetryResult.success((Map<String, Object>) result.get("data"));
        } catch (Exception e) {
            log.error("获取图片信息失败", e);
            return PoetryResult.fail("获取图片信息失败");
        }
    }

    // ========== 私有辅助方法 ==========

    private void clearSeoCache() {
        // 清理静态文件缓存
        seoStaticService.clearStaticCache(null);

        // 清理业务缓存
        cacheService.deleteKeysByPattern("seo:*");

        // 清理搜索引擎推送服务的SEO配置缓存
        searchEnginePushService.clearSeoConfigCache();

        // 清理sitemap缓存
        sitemapService.clearSitemapCache();

    }

    private Map<String, Object> performSeoAnalysis(Map<String, Object> seoConfig) {
        Map<String, Object> analysis = new HashMap<>();

        // 检查基本SEO配置并生成建议
        java.util.List<Map<String, Object>> suggestions = new java.util.ArrayList<>();

        // 检查网站描述
        String description = (String) seoConfig.get("site_description");
        if (!StringUtils.hasText(description) || description.length() < 50) {
            suggestions.add(createSuggestion("warning", "网站描述过短或未设置，建议使用50-160个字符的描述"));
        }

        // 检查关键词
        if (!StringUtils.hasText((String) seoConfig.get("site_keywords"))) {
            suggestions.add(createSuggestion("warning", "网站关键词未设置，这对SEO有一定影响"));
        }

        // 检查搜索引擎推送配置
        checkSearchEngineConfig(suggestions, seoConfig, "baidu_push_enabled", "百度推送功能未启用，建议启用以提高百度搜索引擎收录速度");
        checkSearchEngineConfig(suggestions, seoConfig, "google_index_enabled", "Google索引功能未启用，建议启用以提高Google搜索引擎收录速度");
        checkSearchEngineConfig(suggestions, seoConfig, "bing_push_enabled", "Bing推送功能未启用，建议启用以提高Bing搜索收录速度");

        // 检查网站验证
        checkSiteVerification(suggestions, seoConfig, "baidu_site_verification", "百度站点验证未设置，这会影响百度搜索引擎对网站的信任度");
        checkSiteVerification(suggestions, seoConfig, "google_site_verification",
                "Google站点验证未设置，这会影响对Google Search Console的访问");

        // 计算SEO得分
        int seoScore = Math.max(100 - suggestions.size() * 5, 10);

        analysis.put("suggestions", suggestions);
        analysis.put("seo_score", seoScore);

        return analysis;
    }

    private void checkSearchEngineConfig(java.util.List<Map<String, Object>> suggestions, Map<String, Object> config,
            String key, String message) {
        if (!Boolean.TRUE.equals(config.get(key))) {
            suggestions.add(createSuggestion("warning", message));
        }
    }

    private void checkSiteVerification(java.util.List<Map<String, Object>> suggestions, Map<String, Object> config,
            String key, String message) {
        if (!StringUtils.hasText((String) config.get(key))) {
            suggestions.add(createSuggestion("info", message));
        }
    }

    private Map<String, Object> createSuggestion(String type, String message) {
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("type", type);
        suggestion.put("message", message);
        return suggestion;
    }
}
