package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.ArticleTranslationMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.ArticleTranslation;
import com.ld.poetry.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 翻译服务实现类
 * 通过HTTP调用Python端的翻译服务
 */
@Service
@Slf4j
public class TranslationServiceImpl implements TranslationService {

    @Value("${PYTHON_SERVICE_URL:http://localhost:5000}")
    private String pythonServiceUrl;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTranslationMapper articleTranslationMapper;

    private final RestTemplate restTemplate;

    @Autowired
    private com.ld.poetry.service.SitemapService sitemapService;

    @Autowired
    private com.ld.poetry.service.SysAiConfigService sysAiConfigService;

    public TranslationServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void translateAndSaveArticle(Integer articleId) {
        // 调用新的重载方法，使用默认参数
        translateAndSaveArticle(articleId, false, null);
    }

    @Override
    public void translateAndSaveArticle(Integer articleId, boolean skipAiTranslation,
            Map<String, String> pendingTranslation) {
        log.info("开始翻译并保存文章，ID: {}, 跳过AI翻译: {}, 有暂存翻译: {}",
                articleId, skipAiTranslation, pendingTranslation != null && !pendingTranslation.isEmpty());

        try {
            // 0. 检查翻译配置模式
            com.ld.poetry.entity.SysAiConfig aiConfig = sysAiConfigService.getArticleAiConfig("default");
            boolean isNoneMode = aiConfig != null && "none".equals(aiConfig.getTranslationType());
            boolean hasPendingTranslation = pendingTranslation != null && !pendingTranslation.isEmpty();

            if (isNoneMode && !hasPendingTranslation) {
                log.info("翻译模式为'不翻译'且无手动翻译，跳过翻译处理，文章ID: {}", articleId);
                return;
            }
            if (isNoneMode && hasPendingTranslation) {
                log.info("翻译模式为'不翻译'，但检测到手动编辑的翻译内容，将保存手动翻译，文章ID: {}", articleId);
                // 强制跳过AI翻译，只保存手动翻译
                skipAiTranslation = true;
            }

            // 1. 获取文章内容
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在，ID: {}", articleId);
                return;
            }

            // 检查文章是否有内容
            if (article.getArticleTitle() == null || article.getArticleTitle().trim().isEmpty() ||
                    article.getArticleContent() == null || article.getArticleContent().trim().isEmpty()) {
                log.warn("文章标题或内容为空，跳过翻译，ID: {}", articleId);
                return;
            }

            // 2. 获取翻译配置
            Map<String, Object> defaultLangs = sysAiConfigService.getDefaultLanguages();
            String sourceLanguage = defaultLangs != null
                    ? (String) defaultLangs.getOrDefault("default_source_lang", "zh")
                    : "zh";
            String targetLanguage = defaultLangs != null
                    ? (String) defaultLangs.getOrDefault("default_target_lang", "en")
                    : "en";

            log.info("翻译配置 - 源语言: {}, 目标语言: {}", sourceLanguage, targetLanguage);

            // 3. 处理跳过AI翻译的情况
            if (skipAiTranslation) {
                log.info("跳过AI自动翻译，文章ID: {}", articleId);

                // 如果有暂存的翻译数据，保存它
                if (pendingTranslation != null && !pendingTranslation.isEmpty()) {
                    String translatedTitle = pendingTranslation.get("title");
                    String translatedContent = pendingTranslation.get("content");
                    String translationLanguage = pendingTranslation.get("language");

                    if (translatedTitle != null && translatedContent != null && translationLanguage != null) {
                        boolean success = saveOrUpdateTranslation(articleId, translationLanguage,
                                translatedTitle, translatedContent);
                        if (success) {
                            log.info("暂存翻译保存成功，文章ID: {}, 目标语言: {}，预渲染将由事件监听器自动处理",
                                    articleId, translationLanguage);
                        } else {
                            log.error("暂存翻译保存失败，文章ID: {}, 目标语言: {}", articleId, translationLanguage);
                        }
                    }
                } else {
                    // 没有暂存翻译，预渲染将由事件监听器自动处理
                    log.info("跳过AI翻译且无暂存翻译，预渲染将由事件监听器自动处理，文章ID: {}", articleId);
                }
                return;
            }

            // 4. 翻译文章（使用协程并行翻译标题和内容）
            Map<String, String> translationResult = translateArticleOnly(
                    article.getArticleTitle(),
                    article.getArticleContent(),
                    skipAiTranslation,
                    pendingTranslation);

            // 如果翻译失败或被跳过，直接返回
            if (translationResult == null || translationResult.isEmpty()) {
                log.warn("文章翻译失败或被跳过，文章ID: {}", articleId);
                return;
            }

            String translatedTitle = translationResult.get("title");
            String translatedContent = translationResult.get("content");
            String resultTargetLang = translationResult.get("language");

            // 5. 保存或更新翻译结果（使用事务和重试机制处理并发）
            boolean success = saveOrUpdateTranslation(articleId, resultTargetLang, translatedTitle, translatedContent);

            if (success) {
                log.info("AI翻译保存成功，文章ID: {}, 目标语言: {}，预渲染将由事件监听器自动处理",
                        articleId, resultTargetLang);
            } else {
                log.error("AI翻译保存失败，文章ID: {}, 目标语言: {}", articleId, resultTargetLang);
            }

        } catch (Exception e) {
            log.error("翻译文章失败，文章ID: {}, 错误: {}", articleId, e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> translateArticleOnly(String title, String content, boolean skipAiTranslation,
            Map<String, String> pendingTranslation) {

        try {
            // 0. 检查翻译配置模式
            com.ld.poetry.entity.SysAiConfig aiConfig = sysAiConfigService.getArticleAiConfig("default");
            boolean isNoneMode = aiConfig != null && "none".equals(aiConfig.getTranslationType());
            boolean hasPendingTranslation = pendingTranslation != null && !pendingTranslation.isEmpty();

            if (isNoneMode && !hasPendingTranslation) {
                log.info("翻译模式为'不翻译'且无手动翻译，跳过翻译处理");
                return null;
            }
            if (isNoneMode && hasPendingTranslation) {
                log.info("翻译模式为'不翻译'，但检测到手动编辑的翻译内容，将返回手动翻译");
                return pendingTranslation;
            }

            // 1. 检查文章是否有内容
            if (title == null || title.trim().isEmpty() ||
                    content == null || content.trim().isEmpty()) {
                log.warn("文章标题或内容为空，跳过翻译");
                return null;
            }

            // 2. 获取翻译配置
            Map<String, Object> defaultLangs = sysAiConfigService.getDefaultLanguages();
            String sourceLanguage = defaultLangs != null
                    ? (String) defaultLangs.getOrDefault("default_source_lang", "zh")
                    : "zh";
            String targetLanguage = defaultLangs != null
                    ? (String) defaultLangs.getOrDefault("default_target_lang", "en")
                    : "en";

            log.info("翻译配置 - 源语言: {}, 目标语言: {}", sourceLanguage, targetLanguage);

            // 3. 处理跳过AI翻译的情况
            if (skipAiTranslation) {
                log.info("跳过AI自动翻译");
                // 如果有暂存的翻译数据，返回它
                if (pendingTranslation != null && !pendingTranslation.isEmpty()) {
                    return pendingTranslation;
                }
                return null;
            }

            // 4. 使用TOON格式一次性翻译标题和内容

            // 构建TOON翻译请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", title);
            requestBody.put("content", content);
            requestBody.put("source_lang", sourceLanguage);
            requestBody.put("target_lang", targetLanguage);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "poetize-java");
            headers.set("X-Admin-Request", "true");
            headers.set("User-Agent", "poetize-java/1.0.0");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 调用Python端翻译服务（自动识别TOON格式）
            String url = pythonServiceUrl + "/api/translation/translate";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Integer code = (Integer) responseBody.get("code");

                if (code != null && code == 200) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    if (data != null) {
                        String translatedTitle = (String) data.get("translated_title");
                        String translatedContent = (String) data.get("translated_content");
                        Double tokenSavedPercent = data.get("token_saved_percent") != null
                                ? ((Number) data.get("token_saved_percent")).doubleValue()
                                : null;

                        // 验证翻译结果
                        if (translatedTitle != null && !translatedTitle.trim().isEmpty() &&
                                translatedContent != null && !translatedContent.trim().isEmpty() &&
                                !translatedTitle.equals(title) && !translatedContent.equals(content)) {

                            if (tokenSavedPercent != null && tokenSavedPercent > 0) {
                                log.info("TOON翻译成功！相比传统方式节省了 {}% token",
                                        String.format("%.1f", tokenSavedPercent));
                            } else {
                                log.info("TOON翻译成功");
                            }

                            // 返回翻译结果
                            Map<String, String> result = new HashMap<>();
                            result.put("title", translatedTitle);
                            result.put("content", translatedContent);
                            result.put("language", targetLanguage);

                            return result;
                        } else {
                            log.error("TOON翻译结果无效或未改变");
                            return null;
                        }
                    }
                } else {
                    String message = (String) responseBody.get("message");
                    log.error("TOON翻译失败: {}", message);
                    return null;
                }
            }

            log.error("TOON翻译服务返回异常响应");
            return null;

        } catch (Exception e) {
            log.error("TOON翻译文章失败，错误: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean saveTranslationResult(Integer articleId, String translatedTitle, String translatedContent,
            String targetLanguage) {
        return saveOrUpdateTranslation(articleId, targetLanguage, translatedTitle, translatedContent);
    }

    /**
     * 保存或更新翻译结果，处理并发重复插入问题
     */
    private boolean saveOrUpdateTranslation(Integer articleId, String targetLanguage, String translatedTitle,
            String translatedContent) {
        // 使用重试机制处理并发问题
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 再次检查是否已存在翻译记录（防止并发情况下的重复插入）
                LambdaQueryWrapper<ArticleTranslation> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ArticleTranslation::getArticleId, articleId)
                        .eq(ArticleTranslation::getLanguage, targetLanguage);

                ArticleTranslation existingTranslation = articleTranslationMapper.selectOne(queryWrapper);

                if (existingTranslation != null) {
                    // 更新现有翻译
                    existingTranslation.setTitle(translatedTitle);
                    existingTranslation.setContent(translatedContent);
                    existingTranslation.setUpdateTime(LocalDateTime.now());
                    articleTranslationMapper.updateById(existingTranslation);
                    log.info("更新文章翻译成功，文章ID: {}, 目标语言: {} (尝试第{}次)", articleId, targetLanguage, attempt);

                    // 翻译更新成功后，清除sitemap缓存（翻译URL可能需要更新）
                    updateSitemapForTranslation(articleId, "翻译更新");
                    return true;
                } else {
                    // 创建新翻译
                    ArticleTranslation newTranslation = new ArticleTranslation();
                    newTranslation.setArticleId(articleId);
                    newTranslation.setLanguage(targetLanguage);
                    newTranslation.setTitle(translatedTitle);
                    newTranslation.setContent(translatedContent);
                    newTranslation.setCreateTime(LocalDateTime.now());
                    newTranslation.setUpdateTime(LocalDateTime.now());

                    try {
                        articleTranslationMapper.insert(newTranslation);
                        log.info("创建文章翻译成功，文章ID: {}, 目标语言: {} (尝试第{}次)", articleId, targetLanguage, attempt);

                        // 翻译创建成功后，清除sitemap缓存（新增翻译URL）
                        updateSitemapForTranslation(articleId, "翻译创建");
                        return true;
                    } catch (org.springframework.dao.DuplicateKeyException e) {
                        // 如果遇到重复键异常，说明在我们检查后有其他线程插入了记录
                        log.warn("检测到并发插入，尝试更新现有记录，文章ID: {}, 目标语言: {} (尝试第{}次)", articleId, targetLanguage, attempt);
                        if (attempt < maxRetries) {
                            Thread.sleep(100 * attempt); // 短暂等待后重试
                            continue;
                        } else {
                            // 最后一次尝试：直接尝试更新
                            existingTranslation = articleTranslationMapper.selectOne(queryWrapper);
                            if (existingTranslation != null) {
                                existingTranslation.setTitle(translatedTitle);
                                existingTranslation.setContent(translatedContent);
                                existingTranslation.setUpdateTime(LocalDateTime.now());
                                articleTranslationMapper.updateById(existingTranslation);
                                log.info("最终更新文章翻译成功，文章ID: {}, 目标语言: {}", articleId, targetLanguage);

                                // 翻译最终更新成功后，清除sitemap缓存
                                updateSitemapForTranslation(articleId, "翻译最终更新");
                                return true;
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("翻译保存被中断，文章ID: {}, 目标语言: {}", articleId, targetLanguage);
                return false;
            } catch (Exception e) {
                log.error("保存翻译失败，文章ID: {}, 目标语言: {}, 尝试第{}次, 错误: {}", articleId, targetLanguage, attempt,
                        e.getMessage());
                if (attempt == maxRetries) {
                    return false;
                }
            }
        }

        log.error("保存翻译最终失败，文章ID: {}, 目标语言: {}", articleId, targetLanguage);
        return false;
    }

    /**
     * 删除文章的所有翻译
     */
    public void refreshArticleTranslation(Integer articleId) {
        try {
            LambdaQueryWrapper<ArticleTranslation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ArticleTranslation::getArticleId, articleId);
            int rows = articleTranslationMapper.delete(queryWrapper);
            if (rows > 0) {
                translateAndSaveArticle(articleId); // 重新翻译并将在内部触发 prerender

                // 刷新翻译后，清除sitemap缓存（翻译URL可能发生变化）
                updateSitemapForTranslation(articleId, "刷新翻译");
            }
            log.info("删除文章翻译成功，文章ID: {}", articleId);
        } catch (Exception e) {
            log.error("删除文章翻译失败，文章ID: {}, 错误: {}", articleId, e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getArticleTranslation(Integer articleId, String language) {
        Map<String, String> result = new HashMap<>();

        if (articleId == null || language == null || language.trim().isEmpty()) {
            result.put("error", "文章ID或语言参数无效");
            return result;
        }

        try {
            // 查询文章翻译
            LambdaQueryWrapper<ArticleTranslation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ArticleTranslation::getArticleId, articleId)
                    .eq(ArticleTranslation::getLanguage, language);

            ArticleTranslation translation = articleTranslationMapper.selectOne(queryWrapper);

            if (translation != null) {
                result.put("title", translation.getTitle() != null ? translation.getTitle() : "");
                result.put("content", translation.getContent() != null ? translation.getContent() : "");
                result.put("language", translation.getLanguage());
                result.put("status", "success");
            } else {
                result.put("error", "未找到对应语言的翻译");
                result.put("status", "not_found");
                log.warn("未找到文章翻译，文章ID: {}, 语言: {}", articleId, language);
            }

        } catch (Exception e) {
            log.error("获取文章翻译失败，文章ID: {}, 语言: {}, 错误: {}", articleId, language, e.getMessage(), e);
            result.put("error", "获取翻译失败: " + e.getMessage());
            result.put("status", "error");
        }

        return result;
    }

    @Override
    public List<String> getArticleAvailableLanguages(Integer articleId) {
        List<String> availableLanguages = new ArrayList<>();

        if (articleId == null) {
            log.warn("文章ID为空，无法获取可用翻译语言");
            return availableLanguages;
        }

        try {
            // 查询文章的所有翻译语言
            LambdaQueryWrapper<ArticleTranslation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ArticleTranslation::getArticleId, articleId)
                    .select(ArticleTranslation::getLanguage);

            List<ArticleTranslation> translations = articleTranslationMapper.selectList(queryWrapper);

            if (translations != null && !translations.isEmpty()) {
                availableLanguages = translations.stream()
                        .map(ArticleTranslation::getLanguage)
                        .filter(lang -> lang != null && !lang.trim().isEmpty())
                        .distinct()
                        .collect(Collectors.toList());

            } else {
            }

        } catch (Exception e) {
            log.error("获取文章可用翻译语言失败，文章ID: {}, 错误: {}", articleId, e.getMessage(), e);
        }

        return availableLanguages;
    }

    @Override
    public Map<String, Object> saveManualTranslation(Integer articleId, String targetLanguage,
            String translatedTitle, String translatedContent) {
        Map<String, Object> result = new HashMap<>();

        if (articleId == null || targetLanguage == null || targetLanguage.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "参数无效：文章ID或目标语言不能为空");
            return result;
        }

        if (translatedTitle == null || translatedTitle.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "翻译标题不能为空");
            return result;
        }

        if (translatedContent == null || translatedContent.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "翻译内容不能为空");
            return result;
        }

        try {
            // 检查文章是否存在
            Article article = articleMapper.selectById(articleId);
            if (article == null) {
                result.put("success", false);
                result.put("message", "文章不存在");
                return result;
            }

            // 保存手动翻译
            boolean success = saveOrUpdateTranslation(articleId, targetLanguage,
                    translatedTitle.trim(), translatedContent.trim());

            if (success) {
                result.put("success", true);
                result.put("message", "翻译保存成功");
                log.info("手动翻译保存成功，文章ID: {}, 目标语言: {}", articleId, targetLanguage);
                // 注意：sitemap更新已经在saveOrUpdateTranslation方法中处理
            } else {
                result.put("success", false);
                result.put("message", "翻译保存失败");
                log.error("手动翻译保存失败，文章ID: {}, 目标语言: {}", articleId, targetLanguage);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存翻译时发生错误: " + e.getMessage());
            log.error("手动翻译保存异常，文章ID: {}, 目标语言: {}", articleId, targetLanguage, e);
        }

        return result;
    }

    @Override
    public boolean shouldSkipAutoTranslation(Integer articleId, String targetLanguage) {
        if (articleId == null || targetLanguage == null || targetLanguage.trim().isEmpty()) {
            return false;
        }

        try {
            // 检查是否已存在翻译记录（无论是手动还是自动生成的）
            LambdaQueryWrapper<ArticleTranslation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ArticleTranslation::getArticleId, articleId)
                    .eq(ArticleTranslation::getLanguage, targetLanguage);

            ArticleTranslation existingTranslation = articleTranslationMapper.selectOne(queryWrapper);

            // 如果存在翻译记录，则跳过自动翻译
            boolean shouldSkip = existingTranslation != null;

            if (shouldSkip) {
            }

            return shouldSkip;

        } catch (Exception e) {
            log.error("检查是否跳过自动翻译时发生异常，文章ID: {}, 目标语言: {}", articleId, targetLanguage, e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String translateText(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            // 构建请求参数，设置默认中文转英文
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);
            requestBody.put("source_lang", sourceLang != null ? sourceLang : "zh"); // 默认中文
            requestBody.put("target_lang", targetLang != null ? targetLang : "en"); // 默认英文

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Service", "poetize-java");
            headers.set("X-Admin-Request", "true");
            headers.set("User-Agent", "poetize-java/1.0.0");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 调用Python端翻译服务
            String url = pythonServiceUrl + "/api/translation/translate";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Integer code = (Integer) responseBody.get("code");

                if (code != null && code == 200) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    if (data != null) {
                        String translatedText = (String) data.get("translated_text");
                        if (translatedText != null && !translatedText.trim().isEmpty() &&
                                !translatedText.equals(text)) {
                            return translatedText;
                        } else {
                            log.warn("翻译服务返回无效结果，原文: {}, 翻译结果: {}",
                                    text.length() > 50 ? text.substring(0, 50) + "..." : text,
                                    translatedText != null
                                            ? (translatedText.length() > 50 ? translatedText.substring(0, 50) + "..."
                                                    : translatedText)
                                            : "null");
                        }
                    }
                } else {
                    String message = (String) responseBody.get("message");
                    log.warn("翻译失败: {}", message);
                }
            }

            log.error("翻译服务返回异常响应，原文: {}", text.length() > 50 ? text.substring(0, 50) + "..." : text);
            return null; // 翻译失败时返回null，而不是原文

        } catch (Exception e) {
            log.error("调用翻译服务失败: {}", e.getMessage(), e);
            return null; // 翻译失败时返回null，而不是原文
        }
    }

    @Override
    public void deleteArticleTranslation(Integer articleId) {
        try {
            LambdaQueryWrapper<ArticleTranslation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ArticleTranslation::getArticleId, articleId);
            int rows = articleTranslationMapper.delete(queryWrapper);
            log.info("仅删除文章翻译，无重译，文章ID: {}, 行数: {}", articleId, rows);

            // 删除翻译后，清除sitemap缓存（翻译URL需要从sitemap中移除）
            if (rows > 0) {
                updateSitemapForTranslation(articleId, "删除所有翻译");
            }
        } catch (Exception e) {
            log.error("删除文章翻译失败，文章ID: {}", articleId, e);
        }
    }

    @Override
    public boolean deleteSpecificTranslation(Integer articleId, String language) {
        try {
            LambdaQueryWrapper<ArticleTranslation> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ArticleTranslation::getArticleId, articleId)
                    .eq(ArticleTranslation::getLanguage, language);

            int rows = articleTranslationMapper.delete(queryWrapper);
            log.info("删除文章特定语言翻译，文章ID: {}, 语言: {}, 删除行数: {}", articleId, language, rows);

            // 删除特定语言翻译后，清除sitemap缓存（该语言的翻译URL需要从sitemap中移除）
            if (rows > 0) {
                updateSitemapForTranslation(articleId, "删除" + language + "翻译");
            }

            return rows > 0;
        } catch (Exception e) {
            log.error("删除文章特定语言翻译失败，文章ID: {}, 语言: {}", articleId, language, e);
            return false;
        }
    }

    /**
     * 翻译操作后更新sitemap的辅助方法（只清除缓存）
     * 
     * @param articleId 文章ID
     * @param operation 操作描述
     */
    private void updateSitemapForTranslation(Integer articleId, String operation) {
        try {
            if (sitemapService != null) {
                sitemapService.updateArticleSitemap(articleId);
            }
        } catch (Exception e) {
            log.warn("{}后清除sitemap缓存失败，不影响翻译操作，文章ID: {}, 错误: {}", operation, articleId, e.getMessage());
        }
    }

}