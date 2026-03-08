package com.ld.poetry.service.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ld.poetry.entity.SysAiConfig;
import com.ld.poetry.service.SysAiConfigService;
import com.ld.poetry.utils.ToonFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 翻译与摘要服务
 * 使用 Spring AI ChatClient 替代 Python HTTP 调用
 *
 * 替代 Python 端: translation_api.py 中的 LLM 翻译和摘要生成逻辑
 *
 * 支持的翻译方式：
 * - llm: 使用全局 AI 模型（article_ai 配置的 llmConfig）
 * - dedicated_llm: 使用翻译独立 AI 模型（translationLlmConfig）
 *
 * 支持的摘要方式：
 * - global: 使用全局 AI 模型
 * - dedicated: 使用摘要独立 AI 模型
 */
@Slf4j
@Service
public class LlmTranslationService {

    @Autowired
    private DynamicChatClientFactory chatClientFactory;

    @Autowired
    private SysAiConfigService sysAiConfigService;

    // ==================== 翻译功能 ====================

    /**
     * LLM 翻译文章（标题 + 内容），替代 Python 端的 TOON 翻译
     *
     * @param title      文章标题
     * @param content    文章内容
     * @param sourceLang 源语言代码
     * @param targetLang 目标语言代码
     * @return 翻译结果 {title, content, language} 或 null
     */
    public Map<String, String> translateArticle(String title, String content,
            String sourceLang, String targetLang) {
        try {
            SysAiConfig config = sysAiConfigService.getArticleAiConfigInternal("default");
            if (config == null) {
                log.error("未找到 article_ai 配置");
                return null;
            }

            ChatModel chatModel = createTranslationChatModel(config);
            if (chatModel == null) {
                log.error("无法创建翻译用 ChatModel");
                return null;
            }

            // 构建翻译提示词
            String prompt = buildArticleTranslationPrompt(title, content, sourceLang, targetLang, config);

            log.info("开始 LLM 文章翻译: {} -> {}, 标题长度={}, 内容长度={}",
                    sourceLang, targetLang, title.length(), content.length());

            // 调用 LLM
            String response = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                log.error("LLM 翻译返回空结果");
                return null;
            }

            // 解析翻译结果
            return parseArticleTranslationResponse(response, title, content, targetLang);

        } catch (Exception e) {
            log.error("LLM 文章翻译失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * LLM 翻译纯文本
     *
     * @param text       待翻译文本
     * @param sourceLang 源语言代码
     * @param targetLang 目标语言代码
     * @return 翻译后的文本，失败返回 null
     */
    public String translateText(String text, String sourceLang, String targetLang) {
        try {
            SysAiConfig config = sysAiConfigService.getArticleAiConfigInternal("default");
            if (config == null) {
                log.error("未找到 article_ai 配置");
                return null;
            }

            ChatModel chatModel = createTranslationChatModel(config);
            if (chatModel == null) {
                log.error("无法创建翻译用 ChatModel");
                return null;
            }

            String prompt = String.format("""
                    请将以下文本从 %s 翻译为 %s。
                    只返回翻译结果，不要添加任何解释或注释。

                    原文：
                    %s
                    """, getLanguageName(sourceLang), getLanguageName(targetLang), text);

            String response = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response != null && !response.isBlank() && !response.equals(text)) {
                log.info("LLM 文本翻译成功: {} -> {}", sourceLang, targetLang);
                return response.trim();
            }

            log.warn("LLM 翻译结果无效");
            return null;

        } catch (Exception e) {
            log.error("LLM 文本翻译失败: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 摘要功能 ====================

    /**
     * 生成单语言 AI 摘要
     *
     * @param content   文章内容
     * @param maxLength 摘要最大长度
     * @return 摘要文本，失败返回 null
     */
    public String generateSummary(String content, int maxLength) {
        try {
            SysAiConfig config = sysAiConfigService.getArticleAiConfigInternal("default");
            if (config == null) {
                log.warn("未找到 article_ai 配置，无法生成 AI 摘要");
                return null;
            }

            ChatModel chatModel = createSummaryChatModel(config);
            if (chatModel == null) {
                log.warn("无法创建摘要用 ChatModel");
                return null;
            }

            // 尝试从 summaryConfig 获取自定义 prompt
            String customPromptTemplate = getSummaryCustomPrompt(config);
            String styleDesc = getSummaryStyleDesc(config);
            String prompt;

            if (customPromptTemplate != null && !customPromptTemplate.isBlank()) {
                // 使用自定义提示词，支持占位符替换（与 Python 原版一致）
                prompt = customPromptTemplate
                        .replace("{max_length}", String.valueOf(maxLength))
                        .replace("{style_desc}", styleDesc)
                        .replace("{content_text}", content)
                        .replace("{source_content}", content)
                        .replace("{languages}", "auto")
                        .replace("{source_lang}", "auto");
            } else {
                // 默认提示词（兜底）
                prompt = String.format("""
                        请为以下文章生成一段简洁的摘要。

                        要求：
                        - 摘要长度不超过 %d 个字符
                        - 提取文章的核心要点
                        - 使用与原文相同的语言
                        - 只返回摘要内容，不要添加标题或其他格式

                        文章内容：
                        %s
                        """, maxLength, content);
            }

            String response = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response != null && !response.isBlank()) {
                String summary = response.trim();
                // 截取到目标长度
                if (summary.length() > maxLength) {
                    summary = summary.substring(0, maxLength) + "...";
                }
                log.info("AI 摘要生成成功，长度: {}", summary.length());
                return summary;
            }

            return null;

        } catch (Exception e) {
            log.error("AI 摘要生成失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成多语言摘要
     *
     * @param articleId        文章 ID
     * @param languageContents 各语言内容 { langCode -> {title, content} }
     * @param maxLength        摘要最大长度
     * @return 各语言摘要 { langCode -> summary }，失败返回 null
     */
    public Map<String, String> generateMultiLangSummary(
            Integer articleId, Map<String, Map<String, String>> languageContents, int maxLength) {
        try {
            SysAiConfig config = sysAiConfigService.getArticleAiConfigInternal("default");
            if (config == null) {
                log.warn("未找到 article_ai 配置");
                return null;
            }

            ChatModel chatModel = createSummaryChatModel(config);
            if (chatModel == null) {
                log.warn("无法创建摘要用 ChatModel");
                return null;
            }

            // 收集语言列表和源内容
            StringBuilder languagesStr = new StringBuilder();
            String firstContent = null;
            String firstLangCode = null;
            for (Map.Entry<String, Map<String, String>> entry : languageContents.entrySet()) {
                if (languagesStr.length() > 0)
                    languagesStr.append("、");
                languagesStr.append(getLanguageName(entry.getKey()));
                if (firstContent == null) {
                    firstContent = entry.getValue().get("content");
                    firstLangCode = entry.getKey();
                }
            }

            // 生成 TOON 格式示例（与 Python 原版 toon_encode 一致）
            Map<String, Object> exampleSummaries = new LinkedHashMap<>();
            for (String langCode : languageContents.keySet()) {
                exampleSummaries.put(langCode, getLanguageName(langCode) + "摘要内容");
            }
            Map<String, Object> toonData = new LinkedHashMap<>();
            toonData.put("summaries", exampleSummaries);
            String toonExample = ToonFormatter.encode(toonData);
            String jsonExample = com.alibaba.fastjson.JSON.toJSONString(toonData);
            StringBuilder csvBuilder = new StringBuilder();
            for (Map.Entry<String, Object> e : exampleSummaries.entrySet()) {
                csvBuilder.append(e.getKey()).append(",").append(e.getValue()).append("\n");
            }
            String csvExample = csvBuilder.toString().trim();

            // 尝试从 summaryConfig 获取自定义 prompt
            String customPromptTemplate = getSummaryCustomPrompt(config);
            String styleDesc = getSummaryStyleDesc(config);
            String sourceContent = firstContent != null ? firstContent : "";
            String prompt;

            if (customPromptTemplate != null && !customPromptTemplate.isBlank()) {
                // 使用自定义提示词，支持占位符替换
                prompt = customPromptTemplate
                        .replace("{max_length}", String.valueOf(maxLength))
                        .replace("{style_desc}", styleDesc)
                        .replace("{content_text}", sourceContent)
                        .replace("{source_content}", sourceContent)
                        .replace("{languages}", languagesStr.toString())
                        .replace("{source_lang}", getLanguageName(firstLangCode))
                        .replace("{toon_example}", toonExample)
                        .replace("{json_example}", jsonExample)
                        .replace("{csv_example}", csvExample);
            } else {
                // 默认提示词：使用 TOON 格式（与 Python 原版一致）
                prompt = String.format("""
                        请为以下%s文章生成多语言摘要，要求：
                        1. 生成语言：%s
                        2. 风格：%s
                        3. 每个语言的摘要长度控制在%d字符以内
                        4. 保持TOON格式结构不变（2个空格缩进）
                        5. 只返回TOON格式数据，不添加任何解释或markdown代码块标记
                        6. 注意：为每个目标语言生成该语言的摘要（如需要英文摘要，则生成英文；如需要日文摘要，则生成日文）

                        文章内容：

                        %s

                        请返回TOON格式的摘要，格式如下：
                        %s""",
                        getLanguageName(firstLangCode), languagesStr, styleDesc,
                        maxLength, sourceContent, toonExample);
            }

            String response = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                log.warn("多语言摘要 LLM 返回空结果");
                return null;
            }

            // 解析响应（先尝试 TOON，再 JSON 兜底）
            return parseMultiLangSummaryResponse(response, languageContents, maxLength);

        } catch (Exception e) {
            log.error("多语言摘要生成失败, 文章ID={}: {}", articleId, e.getMessage(), e);
            return null;
        }
    }

    // ==================== ChatModel 创建 ====================

    /**
     * 创建翻译用 ChatModel
     * 优先使用 dedicated_llm（translationLlmConfig），否则使用全局 llmConfig
     */
    private ChatModel createTranslationChatModel(SysAiConfig config) {
        String translationType = config.getTranslationType();

        // dedicated_llm: 使用翻译独立 AI 模型
        if ("dedicated_llm".equals(translationType) && config.getTranslationLlmConfig() != null) {
            return createChatModelFromJson(config.getTranslationLlmConfig(), "翻译独立LLM");
        }

        // llm: 使用全局 AI 模型（llmConfig）
        if ("llm".equals(translationType) && config.getLlmConfig() != null) {
            return createChatModelFromJson(config.getLlmConfig(), "全局LLM");
        }

        // 兼容：尝试使用顶层字段
        if (config.getProvider() != null && config.getApiKey() != null) {
            log.info("使用顶层字段创建翻译 ChatModel");
            return chatClientFactory.createChatModel(config);
        }

        log.error("无可用的翻译 LLM 配置, translationType={}", translationType);
        return null;
    }

    /**
     * 创建摘要用 ChatModel
     * 优先使用 summaryConfig 中的 dedicated_llm，否则使用全局配置
     */
    private ChatModel createSummaryChatModel(SysAiConfig config) {
        if (config.getSummaryConfig() != null) {
            try {
                JSONObject summaryJson = JSON.parseObject(config.getSummaryConfig());
                String summaryMode = summaryJson.getString("summaryMode");

                if ("dedicated".equals(summaryMode)) {
                    JSONObject dedicatedLlm = summaryJson.getJSONObject("dedicated_llm");
                    if (dedicatedLlm != null) {
                        return createChatModelFromJson(dedicatedLlm.toJSONString(), "摘要独立LLM");
                    }
                }
                // global 模式或无 dedicated_llm，降级到全局
            } catch (Exception e) {
                log.warn("解析 summaryConfig 失败，降级到全局配置: {}", e.getMessage());
            }
        }

        // 降级：使用全局 llmConfig 或顶层字段
        if (config.getLlmConfig() != null) {
            return createChatModelFromJson(config.getLlmConfig(), "全局LLM(摘要)");
        }

        if (config.getProvider() != null && config.getApiKey() != null) {
            return chatClientFactory.createChatModel(config);
        }

        log.error("无可用的摘要 LLM 配置");
        return null;
    }

    /**
     * 从 JSON 配置字符串创建 ChatModel
     * JSON 格式: {model, api_url, api_key, interface_type, timeout}
     */
    private ChatModel createChatModelFromJson(String jsonConfig, String label) {
        try {
            JSONObject json = JSON.parseObject(jsonConfig);

            // 构建临时 SysAiConfig 对象传给 factory
            SysAiConfig tempConfig = new SysAiConfig();
            tempConfig.setModel(json.getString("model"));
            tempConfig.setApiBase(json.getString("api_url"));
            tempConfig.setApiKey(json.getString("api_key"));

            // interface_type 映射到 provider
            String interfaceType = json.getString("interface_type");
            if (interfaceType != null) {
                tempConfig.setProvider(switch (interfaceType.toLowerCase()) {
                    case "openai", "openai_compatible" -> "openai";
                    case "anthropic" -> "anthropic";
                    default -> "openai"; // 默认 OpenAI 兼容
                });
            } else {
                tempConfig.setProvider("openai");
            }

            // 翻译使用较低温度确保准确性
            tempConfig.setTemperature(new java.math.BigDecimal("0.3"));

            log.info("从 JSON 创建 {} ChatModel: provider={}, model={}",
                    label, tempConfig.getProvider(), tempConfig.getModel());

            return chatClientFactory.createChatModel(tempConfig);

        } catch (Exception e) {
            log.error("从 JSON 创建 {} ChatModel 失败: {}", label, e.getMessage(), e);
            return null;
        }
    }

    // ==================== 摘要 Prompt 辅助方法 ====================

    /**
     * 从 summaryConfig JSON 中提取自定义 prompt 模板
     * 对应 Python 原版: summary_config.get('prompt')
     */
    private String getSummaryCustomPrompt(SysAiConfig config) {
        if (config.getSummaryConfig() == null)
            return null;
        try {
            JSONObject summaryJson = JSON.parseObject(config.getSummaryConfig());
            return summaryJson.getString("prompt");
        } catch (Exception e) {
            log.warn("解析 summaryConfig 获取 prompt 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 summaryConfig JSON 中获取摘要风格描述
     * 对应 Python 原版: style_prompts.get(request.style)
     */
    private String getSummaryStyleDesc(SysAiConfig config) {
        String style = "concise";
        if (config.getSummaryConfig() != null) {
            try {
                JSONObject summaryJson = JSON.parseObject(config.getSummaryConfig());
                String configStyle = summaryJson.getString("style");
                if (configStyle != null && !configStyle.isBlank()) {
                    style = configStyle;
                }
            } catch (Exception ignored) {
            }
        }
        return switch (style) {
            case "detailed" -> "详细全面，包含文章的主要内容和关键信息";
            case "academic" -> "学术风格，使用专业术语和结构化表达";
            default -> "简洁明了，突出文章的核心观点";
        };
    }

    // ==================== 翻译辅助方法 ====================

    /**
     * 构建文章翻译提示词
     */
    private String buildArticleTranslationPrompt(String title, String content,
            String sourceLang, String targetLang,
            SysAiConfig config) {
        // 尝试从 llmConfig 获取自定义 prompt
        String customPrompt = null;
        String llmJsonConfig = "dedicated_llm".equals(config.getTranslationType())
                ? config.getTranslationLlmConfig()
                : config.getLlmConfig();

        if (llmJsonConfig != null) {
            try {
                JSONObject json = JSON.parseObject(llmJsonConfig);
                customPrompt = json.getString("prompt");
            } catch (Exception ignored) {
            }
        }

        if (customPrompt != null && !customPrompt.isBlank()) {
            // 生成不同的格式数据供占位符使用
            Map<String, Object> toonDataMap = new LinkedHashMap<>();
            toonDataMap.put("title", title);
            toonDataMap.put("content", content);
            String toonData = ToonFormatter.encode(toonDataMap);
            String jsonData = JSON.toJSONString(toonDataMap);
            String csvData = String.format("title,content\n\"%s\",\"%s\"", title.replace("\"", "\"\""),
                    content.replace("\"", "\"\""));

            // 使用自定义提示词，替换变量
            return customPrompt
                    .replace("{source_lang}", getLanguageName(sourceLang))
                    .replace("{target_lang}", getLanguageName(targetLang))
                    .replace("{title}", title)
                    .replace("{content}", content)
                    .replace("{toon_data}", toonData)
                    .replace("{json_data}", jsonData)
                    .replace("{csv_data}", csvData)
                    .replace("{format}", "JSON格式"); // 默认建议的 format
        }

        // 默认提示词
        return String.format("""
                你是一个专业翻译，请将以下文章从 %s 精确翻译为 %s。

                翻译要求：
                1. 保持原文的格式和语义
                2. 如果内容包含 Markdown 格式，保留 Markdown 标记
                3. 如果内容包含代码块，代码本身不要翻译，只翻译注释和说明文字
                4. 保留原文中的链接、图片等引用不变
                5. 请分别翻译标题和内容

                请严格按照以下 JSON 格式返回，不要添加任何其他内容：
                {"translated_title": "翻译后的标题", "translated_content": "翻译后的内容"}

                === 原文标题 ===
                %s

                === 原文内容 ===
                %s
                """, getLanguageName(sourceLang), getLanguageName(targetLang), title, content);
    }

    /**
     * 解析文章翻译 LLM 响应
     */
    private Map<String, String> parseArticleTranslationResponse(
            String response, String originalTitle, String originalContent, String targetLang) {
        try {
            // 尝试提取 JSON
            String jsonStr = extractJson(response);
            if (jsonStr != null) {
                JSONObject json = JSON.parseObject(jsonStr);
                String translatedTitle = json.getString("translated_title");
                String translatedContent = json.getString("translated_content");

                if (translatedTitle != null && !translatedTitle.isBlank()
                        && translatedContent != null && !translatedContent.isBlank()
                        && !translatedTitle.equals(originalTitle)
                        && !translatedContent.equals(originalContent)) {

                    Map<String, String> result = new HashMap<>();
                    result.put("title", translatedTitle);
                    result.put("content", translatedContent);
                    result.put("language", targetLang);
                    log.info("LLM 文章翻译解析成功");
                    return result;
                }
            }

            // JSON 解析失败，尝试分段提取
            log.warn("LLM 翻译结果非标准 JSON，尝试分段提取");

            // 简单的分段提取：查找标题和内容分隔
            int titleEnd = response.indexOf("\n\n");
            if (titleEnd > 0 && titleEnd < response.length() - 10) {
                String translatedTitle = response.substring(0, titleEnd).trim();
                String translatedContent = response.substring(titleEnd + 2).trim();

                if (!translatedTitle.equals(originalTitle) && !translatedContent.equals(originalContent)) {
                    Map<String, String> result = new HashMap<>();
                    result.put("title", translatedTitle);
                    result.put("content", translatedContent);
                    result.put("language", targetLang);
                    return result;
                }
            }

            log.error("LLM 翻译结果解析失败");
            return null;

        } catch (Exception e) {
            log.error("解析翻译响应失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析多语言摘要 LLM 响应
     * 先尝试 TOON 格式解析（与 Python 原版一致），再 JSON 兜底
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parseMultiLangSummaryResponse(
            String response, Map<String, Map<String, String>> languageContents, int maxLength) {

        // 1. 先尝试 TOON 格式解析（Python 原版默认格式）
        try {
            Map<String, Object> toonDecoded = ToonFormatter.decode(response);
            if (toonDecoded != null) {
                // TOON 摘要格式: summaries: { zh: "...", en: "..." }
                Object summariesObj = toonDecoded.get("summaries");
                if (summariesObj instanceof Map) {
                    Map<String, String> result = extractSummaries(
                            (Map<String, Object>) summariesObj, languageContents, maxLength);
                    if (result != null) {
                        log.info("TOON 格式摘要解析成功，包含 {} 个语言", result.size());
                        return result;
                    }
                }
                // TOON 解码成功但没有 summaries 字段，可能是扁平结构 {zh: "...", en: "..."}
                Map<String, String> flatResult = extractSummaries(
                        toonDecoded, languageContents, maxLength);
                if (flatResult != null) {
                    log.info("TOON 扁平格式摘要解析成功，包含 {} 个语言", flatResult.size());
                    return flatResult;
                }
            }
        } catch (Exception e) {
            log.debug("TOON 解析失败，尝试 JSON: {}", e.getMessage());
        }

        // 2. JSON 兜底（用户可能自定义了 JSON 格式的 prompt）
        try {
            String jsonStr = extractJson(response);
            if (jsonStr != null) {
                Map<String, Object> jsonMap = JSON.parseObject(jsonStr, Map.class);
                // 检查是否有 summaries 嵌套
                Object summariesObj = jsonMap.get("summaries");
                if (summariesObj instanceof Map) {
                    Map<String, String> result = extractSummaries(
                            (Map<String, Object>) summariesObj, languageContents, maxLength);
                    if (result != null) {
                        log.info("JSON summaries 格式摘要解析成功，包含 {} 个语言", result.size());
                        return result;
                    }
                }
                // 扁平 JSON: {"zh": "...", "en": "..."}
                Map<String, String> flatResult = extractSummaries(
                        jsonMap, languageContents, maxLength);
                if (flatResult != null) {
                    log.info("JSON 扁平格式摘要解析成功，包含 {} 个语言", flatResult.size());
                    return flatResult;
                }
            }
        } catch (Exception e) {
            log.debug("JSON 解析也失败: {}", e.getMessage());
        }

        // 3. 键值对 & CSV / TSV 兜底（针对缩进丢失、逗号/制表符/冒号分割的纯文本）
        try {
            Map<String, String> csvResult = new LinkedHashMap<>();
            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                // 忽略空白行、markdown块标记以及可能的表头
                if (line.isBlank() || line.startsWith("```") || line.toLowerCase().startsWith("lang")
                        || line.toLowerCase().startsWith("summar")) {
                    continue;
                }
                // 支持 CSV/TSV 以及缩进丢失的 TOON（如 zh: summary 或 zh=summary）
                String[] parts = line.split("[,，\\t:]", 2);
                if (parts.length < 2) {
                    parts = line.split("：", 2); // 中文冒号
                }
                if (parts.length < 2) {
                    parts = line.split("=", 2); // 等号
                }

                if (parts.length == 2) {
                    String lang = parts[0].replace("\"", "").replace("'", "").trim();
                    String summary = parts[1].replaceAll("^[\"']|[\"']$", "").trim();
                    if (languageContents.containsKey(lang) && !summary.isBlank()) {
                        if (summary.length() > maxLength) {
                            summary = summary.substring(0, maxLength) + "...";
                        }
                        csvResult.put(lang, summary.trim());
                    }
                }
            }
            if (!csvResult.isEmpty()) {
                log.info("CSV 格式摘要解析成功，包含 {} 个语言", csvResult.size());
                return csvResult;
            }
        } catch (Exception e) {
            log.debug("CSV 解析也失败: {}", e.getMessage());
        }

        log.warn("多语言摘要解析失败（TOON、JSON 和 CSV 均无法解析）");
        return null;
    }

    /**
     * 从解析后的 Map 中提取摘要，验证语言代码并截取长度
     */
    private Map<String, String> extractSummaries(
            Map<String, Object> source, Map<String, Map<String, String>> languageContents, int maxLength) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String langCode = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String summary && languageContents.containsKey(langCode)
                    && !summary.isBlank()) {
                if (summary.length() > maxLength) {
                    summary = summary.substring(0, maxLength) + "...";
                }
                result.put(langCode, summary.trim());
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 从 LLM 响应中提取 JSON 字符串
     */
    private String extractJson(String text) {
        if (text == null)
            return null;
        text = text.trim();

        // 移除 markdown 代码块标记
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        // 查找 JSON 对象
        int braceStart = text.indexOf('{');
        int braceEnd = text.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return text.substring(braceStart, braceEnd + 1);
        }

        return null;
    }

    /**
     * 语言代码映射到语言名称
     */
    private String getLanguageName(String langCode) {
        if (langCode == null)
            return "中文";
        return switch (langCode.toLowerCase()) {
            case "zh", "zh-cn", "zh-hans" -> "中文";
            case "zh-tw", "zh-TW", "zh-hk", "zh-HK", "zh-hant", "zh-Hant" -> "繁体中文";
            case "en" -> "英文";
            case "ja" -> "日文";
            case "ko" -> "韩文";
            case "fr" -> "法文";
            case "de" -> "德文";
            case "es" -> "西班牙文";
            case "pt" -> "葡萄牙文";
            case "ru" -> "俄文";
            case "ar" -> "阿拉伯文";
            default -> langCode;
        };
    }
}
