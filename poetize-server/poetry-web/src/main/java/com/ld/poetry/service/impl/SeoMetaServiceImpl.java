package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ld.poetry.service.SeoMetaService;
import com.ld.poetry.service.SeoConfigService;
import com.ld.poetry.service.ArticleService;
import com.ld.poetry.service.SortService;
import com.ld.poetry.service.LabelService;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.dao.ArticleTranslationMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.ArticleTranslation;
import com.ld.poetry.entity.Sort;
import com.ld.poetry.entity.Label;
import com.ld.poetry.utils.PoetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * SEO元数据生成服务实现类
 * </p>
 *
 * @author LeapYa
 * @since 2025-9-25
 */
@Service
@Slf4j
public class SeoMetaServiceImpl implements SeoMetaService {

    @Autowired
    private SeoConfigService seoConfigService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private SortService sortService;

    @Autowired
    private LabelService labelService;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private ArticleTranslationMapper articleTranslationMapper;
    
    @Autowired
    private com.ld.poetry.service.SysAiConfigService sysAiConfigService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    
    @Autowired
    private com.ld.poetry.utils.mail.MailUtil mailUtil;

    // HTML标签清理的正则表达式
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    @Override
    public Map<String, Object> generateArticleMeta(Integer articleId, String language) {
        try {
            Map<String, Object> seoConfig = seoConfigService.getSeoConfigAsJson();
            Map<String, Object> meta = new HashMap<>();

            // 检查SEO是否启用
            if (!Boolean.TRUE.equals(seoConfig.get("enable"))) {
                return createDisabledMeta();
            }

            // 获取文章信息
            Article article = articleService.getById(articleId);
            if (article == null) {
                return createNotFoundMeta();
            }

            // 基础元数据
            String title = StringUtils.hasText(article.getArticleTitle()) ? 
                article.getArticleTitle() : getSiteTitle();
            
            // 生成描述时传入语言参数，使用对应语言的摘要
            String description = generateArticleDescription(article, seoConfig, language);
            String keywords = generateArticleKeywords(article, seoConfig);

            meta.put("title", title);
            meta.put("description", description);
            meta.put("keywords", keywords);
            meta.put("author", StringUtils.hasText(seoConfig.get("default_author").toString()) ? 
                seoConfig.get("default_author") : getSiteTitle());

            // 文章特定信息
            meta.put("article_title", article.getArticleTitle());
            meta.put("article_id", articleId);
            meta.put("published_time", article.getCreateTime());
            meta.put("modified_time", article.getUpdateTime());

            // 获取分类信息
            if (article.getSortId() != null) {
                Sort sort = sortService.getById(article.getSortId());
                if (sort != null) {
                    meta.put("category", sort.getSortName());
                    meta.put("category_id", sort.getId());
                }
            }

            // 获取标签信息
            if (article.getLabelId() != null) {
                Label label = labelService.getById(article.getLabelId());
                if (label != null) {
                    meta.put("tag", label.getLabelName());
                    meta.put("tag_id", label.getId());
                }
            }

            // OpenGraph和Twitter Card
            addSocialMediaMeta(meta, seoConfig, title, description);

            // Canonical URL (规范链接)
            String siteUrl = mailUtil.getSiteUrl();
            if (StringUtils.hasText(siteUrl)) {
                meta.put("canonical", siteUrl + "/article/" + articleId);
                meta.put("og:url", siteUrl + "/article/" + articleId);
            }

            // 获取文章封面图（与前端展示逻辑一致：优先用文章封面，否则用随机封面）
            String articleImage = getArticleCoverUrl(article);
            if (StringUtils.hasText(articleImage)) {
                meta.put("og:image", articleImage);
            }

            // 自定义头部代码
            meta.put("custom_head_code", seoConfig.get("custom_head_code"));

            // 结构化数据
            meta.put("structured_data", generateArticleStructuredData(article, seoConfig));

            return meta;

        } catch (Exception e) {
            log.error("生成文章SEO元数据失败: articleId={}", articleId, e);
            return createErrorMeta();
        }
    }

    @Override
    public Map<String, Object> generateSiteMeta(String language) {
        try {
            Map<String, Object> seoConfig = seoConfigService.getSeoConfigAsJson();
            Map<String, Object> meta = new HashMap<>();

            // 检查SEO是否启用
            if (!Boolean.TRUE.equals(seoConfig.get("enable"))) {
                return createDisabledMeta();
            }

            // 基础站点信息
            meta.put("title", getSiteTitle());
            meta.put("description", seoConfig.get("site_description"));
            meta.put("keywords", seoConfig.get("site_keywords"));
            meta.put("author", seoConfig.get("default_author"));
            meta.put("site_name", seoConfig.get("og_site_name"));

            // 网站图标
            addIconMeta(meta, seoConfig);

            // OpenGraph和Twitter Card
            addSocialMediaMeta(meta, seoConfig, 
                getSiteTitle(), 
                seoConfig.get("site_description").toString());

            // Canonical URL (规范链接)
            String siteUrl = mailUtil.getSiteUrl();
            if (StringUtils.hasText(siteUrl)) {
                meta.put("canonical", siteUrl);
                meta.put("og:url", siteUrl);
            }

            // PWA相关
            addPwaMeta(meta, seoConfig);

            // 自定义头部代码
            meta.put("custom_head_code", seoConfig.get("custom_head_code"));

            // 结构化数据
            meta.put("structured_data", generateWebsiteStructuredData(seoConfig));

            return meta;

        } catch (Exception e) {
            log.error("生成网站SEO元数据失败", e);
            return createErrorMeta();
        }
    }

    @Override
    public Map<String, Object> generateCategoryMeta(Integer categoryId, String language) {
        try {
            Map<String, Object> seoConfig = seoConfigService.getSeoConfigAsJson();
            Map<String, Object> meta = new HashMap<>();

            // 检查SEO是否启用
            if (!Boolean.TRUE.equals(seoConfig.get("enable"))) {
                return createDisabledMeta();
            }

            // 获取分类信息
            Sort category = sortService.getById(categoryId);
            if (category == null) {
                return createNotFoundMeta();
            }

            String title = category.getSortName() + " - " + getSiteTitle();
            String description = StringUtils.hasText(category.getSortDescription()) ?
                category.getSortDescription() : 
                "查看 " + category.getSortName() + " 分类下的所有文章 - " + seoConfig.get("site_description");

            meta.put("title", title);
            meta.put("description", description);
            meta.put("keywords", category.getSortName() + "," + seoConfig.get("site_keywords"));
            meta.put("category_name", category.getSortName());
            meta.put("category_id", categoryId);

            // OpenGraph和Twitter Card
            addSocialMediaMeta(meta, seoConfig, title, description);

            // Canonical URL (规范链接)
            String siteUrl = mailUtil.getSiteUrl();
            if (StringUtils.hasText(siteUrl)) {
                meta.put("canonical", siteUrl + "/sort/" + categoryId);
                meta.put("og:url", siteUrl + "/sort/" + categoryId);
            }

            // 自定义头部代码
            meta.put("custom_head_code", seoConfig.get("custom_head_code"));

            // 结构化数据
            meta.put("structured_data", generateCategoryStructuredData(category, seoConfig));

            return meta;

        } catch (Exception e) {
            log.error("生成分类SEO元数据失败: categoryId={}", categoryId, e);
            return createErrorMeta();
        }
    }

    @Override
    public Map<String, Object> generateTagMeta(Integer tagId, String language) {
        try {
            Map<String, Object> seoConfig = seoConfigService.getSeoConfigAsJson();
            Map<String, Object> meta = new HashMap<>();

            // 检查SEO是否启用
            if (!Boolean.TRUE.equals(seoConfig.get("enable"))) {
                return createDisabledMeta();
            }

            // 获取标签信息
            Label tag = labelService.getById(tagId);
            if (tag == null) {
                return createNotFoundMeta();
            }

            String title = tag.getLabelName() + " - " + getSiteTitle();
            String description = StringUtils.hasText(tag.getLabelDescription()) ?
                tag.getLabelDescription() : 
                "查看标签 " + tag.getLabelName() + " 下的所有文章 - " + seoConfig.get("site_description");

            meta.put("title", title);
            meta.put("description", description);
            meta.put("keywords", tag.getLabelName() + "," + seoConfig.get("site_keywords"));
            meta.put("tag_name", tag.getLabelName());
            meta.put("tag_id", tagId);

            // OpenGraph和Twitter Card
            addSocialMediaMeta(meta, seoConfig, title, description);

            // Canonical URL (规范链接)
            String siteUrl = mailUtil.getSiteUrl();
            if (StringUtils.hasText(siteUrl)) {
                meta.put("canonical", siteUrl + "/tag/" + tagId);
                meta.put("og:url", siteUrl + "/tag/" + tagId);
            }

            // 自定义头部代码
            meta.put("custom_head_code", seoConfig.get("custom_head_code"));

            return meta;

        } catch (Exception e) {
            log.error("生成标签SEO元数据失败: tagId={}", tagId, e);
            return createErrorMeta();
        }
    }

    @Override
    public Map<String, Object> generateImSiteMeta(String language) {
        try {
            Map<String, Object> seoConfig = seoConfigService.getSeoConfigAsJson();
            Map<String, Object> meta = new HashMap<>();

            // 检查SEO是否启用
            if (!Boolean.TRUE.equals(seoConfig.get("enable"))) {
                return createDisabledMeta();
            }

            String title = "即时通讯 - " + getSiteTitle();
            String description = "在线聊天和即时通讯功能 - " + seoConfig.get("site_description");

            meta.put("title", title);
            meta.put("description", description);
            meta.put("keywords", "即时通讯,在线聊天,IM," + seoConfig.get("site_keywords"));

            // OpenGraph和Twitter Card
            addSocialMediaMeta(meta, seoConfig, title, description);

            // 自定义头部代码
            meta.put("custom_head_code", seoConfig.get("custom_head_code"));

            return meta;

        } catch (Exception e) {
            log.error("生成IM站点SEO元数据失败", e);
            return createErrorMeta();
        }
    }

    @Override
    public Map<String, Object> detectSiteUrl(HttpServletRequest request) {
        try {
            // 从请求头检测URL
            String detectedUrl = detectUrlFromRequest(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("detected_url", detectedUrl);
            result.put("fallback_url", "http://localhost");
            result.put("detection_source", "request_headers");

            log.info("检测到网站URL: {}", detectedUrl);
            return result;

        } catch (Exception e) {
            log.error("检测网站URL失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("detected_url", "http://localhost");
            result.put("fallback_url", "http://localhost");
            result.put("detection_source", "fallback");
            return result;
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 生成文章描述（支持多语言）
     * 
     * @param article 文章对象
     * @param seoConfig SEO配置
     * @param language 语言代码，如果为null则使用源语言
     * @return 文章描述
     */
    private String generateArticleDescription(Article article, Map<String, Object> seoConfig, String language) {
        String description = "";
        
        // 如果指定了语言参数，尝试从翻译表获取对应语言的摘要
        if (StringUtils.hasText(language)) {
            try {
                // 获取默认源语言
                Map<String, Object> defaultLangs = sysAiConfigService.getDefaultLanguages();
                String sourceLanguage = defaultLangs != null ? 
                    (String) defaultLangs.getOrDefault("default_source_lang", "zh") : "zh";
                
                // 如果不是源语言，从翻译表获取摘要
                if (!language.equals(sourceLanguage)) {
                    ArticleTranslation translation = articleTranslationMapper.selectOne(
                        new LambdaQueryWrapper<ArticleTranslation>()
                            .eq(ArticleTranslation::getArticleId, article.getId())
                            .eq(ArticleTranslation::getLanguage, language)
                    );
                    
                    if (translation != null && StringUtils.hasText(translation.getSummary())) {
                        description = cleanHtmlTags(translation.getSummary());
                        return description;
                    } else {
                    }
                }
            } catch (Exception e) {
                log.warn("获取翻译摘要失败，使用原文摘要: {}", e.getMessage());
            }
        }
        
        // 使用源语言摘要或内容
        if (StringUtils.hasText(article.getSummary())) {
            description = cleanHtmlTags(article.getSummary());
        } else if (StringUtils.hasText(article.getArticleContent())) {
            // 从内容中提取描述
            String content = cleanHtmlTags(article.getArticleContent());
            description = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }

        // 如果还是没有，使用网站默认描述
        if (!StringUtils.hasText(description)) {
            description = seoConfig.get("site_description").toString();
        }

        return description;
    }

    private String generateArticleKeywords(Article article, Map<String, Object> seoConfig) {
        StringBuilder keywords = new StringBuilder();

        // 添加文章标题中的关键词
        if (StringUtils.hasText(article.getArticleTitle())) {
            String[] titleWords = article.getArticleTitle().split("[\\s,，、]+");
            for (String word : titleWords) {
                if (word.length() > 1) {
                    keywords.append(word).append(",");
                }
            }
        }

        // 添加网站关键词
        if (seoConfig.get("site_keywords") != null) {
            keywords.append(seoConfig.get("site_keywords"));
        }

        return keywords.toString().replaceAll(",$", "");
    }

    private void addSocialMediaMeta(Map<String, Object> meta, Map<String, Object> seoConfig, String title, String description) {
        // OpenGraph
        meta.put("og:type", seoConfig.get("og_type"));
        meta.put("og:title", title);
        meta.put("og:description", description);
        meta.put("og:site_name", seoConfig.get("og_site_name"));
        meta.put("og:image", seoConfig.get("og_image"));

        // Twitter Card
        meta.put("twitter:card", seoConfig.get("twitter_card"));
        meta.put("twitter:title", title);
        meta.put("twitter:description", description);
        meta.put("twitter:site", seoConfig.get("twitter_site"));
        meta.put("twitter:creator", seoConfig.get("twitter_creator"));
    }

    /**
     * 获取文章封面图URL（与前端buildArticleVO逻辑一致）
     * 优先使用文章自身封面，如果为空则使用随机封面
     */
    private String getArticleCoverUrl(Article article) {
        if (StringUtils.hasText(article.getArticleCover())) {
            return article.getArticleCover();
        }
        // 与 ArticleServiceImpl.buildArticleVO 保持一致，使用文章ID生成随机封面
        return PoetryUtil.getRandomCover(article.getId().toString());
    }

    private void addIconMeta(Map<String, Object> meta, Map<String, Object> seoConfig) {
        meta.put("site_icon", seoConfig.get("site_icon"));
        meta.put("site_icon_192", seoConfig.get("site_icon_192"));
        meta.put("site_icon_512", seoConfig.get("site_icon_512"));
        meta.put("apple_touch_icon", seoConfig.get("apple_touch_icon"));
        meta.put("site_logo", seoConfig.get("site_logo"));
    }

    private void addPwaMeta(Map<String, Object> meta, Map<String, Object> seoConfig) {
        meta.put("pwa_theme_color", seoConfig.get("pwa_theme_color"));
        meta.put("pwa_background_color", seoConfig.get("pwa_background_color"));
        meta.put("pwa_display", seoConfig.get("pwa_display"));
    }

    private String generateArticleStructuredData(Article article, Map<String, Object> seoConfig) {
        // 生成JSON-LD结构化数据
        Map<String, Object> structuredData = new HashMap<>();
        structuredData.put("@context", "https://schema.org");
        structuredData.put("@type", "Article");
        structuredData.put("headline", article.getArticleTitle());
        structuredData.put("datePublished", article.getCreateTime());
        structuredData.put("dateModified", article.getUpdateTime());

        // 文章封面图片 (image) 
        String articleImage = getArticleCoverUrl(article);
        if (StringUtils.hasText(articleImage)) {
            structuredData.put("image", articleImage);
        } else if (seoConfig.get("og_image") != null && StringUtils.hasText(seoConfig.get("og_image").toString())) {
            // 最终降级使用全局默认OG图片
            structuredData.put("image", seoConfig.get("og_image").toString());
        }

        // mainEntityOfPage - 指向文章的规范URL
        String siteUrl = mailUtil.getSiteUrl();
        if (StringUtils.hasText(siteUrl)) {
            Map<String, Object> mainEntity = new HashMap<>();
            mainEntity.put("@type", "WebPage");
            mainEntity.put("@id", siteUrl + "/article/" + article.getId());
            structuredData.put("mainEntityOfPage", mainEntity);
        }

        // 文章描述
        if (StringUtils.hasText(article.getSummary())) {
            structuredData.put("description", cleanHtmlTags(article.getSummary()));
        }

        Map<String, Object> author = new HashMap<>();
        author.put("@type", "Person");
        author.put("name", seoConfig.get("default_author"));
        structuredData.put("author", author);

        // Publisher - 使用与og:site_name一致的名称，保持品牌一致性
        String siteName = getSiteTitle();
        Map<String, Object> publisher = new HashMap<>();
        publisher.put("@type", "Organization");
        publisher.put("name", siteName);
        // Publisher logo
        if (seoConfig.get("site_logo") != null && StringUtils.hasText(seoConfig.get("site_logo").toString())) {
            Map<String, Object> logo = new HashMap<>();
            logo.put("@type", "ImageObject");
            logo.put("url", seoConfig.get("site_logo").toString());
            publisher.put("logo", logo);
        }
        structuredData.put("publisher", publisher);

        return toJsonString(structuredData);
    }

    private String generateWebsiteStructuredData(Map<String, Object> seoConfig) {
        Map<String, Object> structuredData = new HashMap<>();
        structuredData.put("@context", "https://schema.org");
        structuredData.put("@type", "WebSite");
        structuredData.put("name", getSiteTitle());
        structuredData.put("description", seoConfig.get("site_description"));
        // 直接从 MailUtil 获取网站地址
        structuredData.put("url", mailUtil.getSiteUrl());

        return toJsonString(structuredData);
    }

    private String generateCategoryStructuredData(Sort category, Map<String, Object> seoConfig) {
        Map<String, Object> structuredData = new HashMap<>();
        structuredData.put("@context", "https://schema.org");
        structuredData.put("@type", "CollectionPage");
        structuredData.put("name", category.getSortName());
        structuredData.put("description", category.getSortDescription());

        return toJsonString(structuredData);
    }

    private String cleanHtmlTags(String htmlContent) {
        if (!StringUtils.hasText(htmlContent)) {
            return "";
        }
        return HTML_TAG_PATTERN.matcher(htmlContent).replaceAll("").trim();
    }

    private String detectUrlFromRequest(HttpServletRequest request) {
        // 检测协议
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (!StringUtils.hasText(scheme)) {
            scheme = request.getScheme();
        }

        // 检测主机
        String host = request.getHeader("X-Forwarded-Host");
        if (!StringUtils.hasText(host)) {
            host = request.getHeader("Host");
        }
        if (!StringUtils.hasText(host)) {
            host = request.getServerName();
            int port = request.getServerPort();
            if (port != 80 && port != 443) {
                host = host + ":" + port;
            }
        }

        return scheme + "://" + host;
    }

    private Map<String, Object> createDisabledMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("seo_enabled", false);
        meta.put("message", "SEO功能已禁用");
        return meta;
    }

    private Map<String, Object> createNotFoundMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("error", "资源未找到");
        meta.put("status", 404);
        return meta;
    }

    private Map<String, Object> createErrorMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("error", "生成SEO元数据时发生错误");
        meta.put("status", 500);
        return meta;
    }

    private String toJsonString(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("转换JSON字符串失败", e);
            return "{}";
        }
    }
    
    /**
     * 获取网站标题，优先使用webInfo.webTitle，然后尝试SEO配置中的og_site_name
     */
    private String getSiteTitle() {
        try {
            var webInfo = cacheService.getCachedWebInfo();
            if (webInfo != null && StringUtils.hasText(webInfo.getWebTitle())) {
                return webInfo.getWebTitle();
            } else if (webInfo != null && StringUtils.hasText(webInfo.getWebName())) {
                return webInfo.getWebName();
            }
        } catch (Exception e) {
            log.warn("获取webInfo失败，尝试从SEO配置获取站点名称", e);
        }
        // 尝试从SEO配置获取og_site_name作为备选
        try {
            Map<String, Object> seoConfig = seoConfigService.getSeoConfigAsJson();
            Object ogSiteName = seoConfig.get("og_site_name");
            if (ogSiteName != null && StringUtils.hasText(ogSiteName.toString())) {
                return ogSiteName.toString();
            }
        } catch (Exception e) {
            log.warn("获取SEO配置失败", e);
        }
        return "My Blog";
    }
}
