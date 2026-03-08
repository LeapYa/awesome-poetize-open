package com.ld.poetry.controller;

import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.service.SysAiConfigService;
import com.ld.poetry.service.ai.DynamicChatClientFactory;
import com.ld.poetry.entity.SysAiConfig;
import com.ld.poetry.utils.ToonFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 翻译测试 API 端点
 * <p>
 * 提供翻译/摘要功能的测试和调试接口。
 * 兼容前端 translationModelManage.vue 发来的 tempConfig 格式。
 * 仅管理员可用（通过 LoginCheck 拦截器保护）。
 */
@RestController
@RequestMapping("/admin/translation")
@Slf4j
public class TranslationApiController {

    @Autowired
    private DynamicChatClientFactory chatClientFactory;

    @Autowired
    private SysAiConfigService sysAiConfigService;

    // ========== 前端 translationModelManage.vue 兼容端点 ==========

    /**
     * 翻译测试（兼容前端 tempConfig 格式）
     * <p>
     * 前端调用格式:
     * POST /admin/translation/test/text
     * Body: { config: {type, llm: {...}, ...}, text: "...", title: "...", content:
     * "..." }
     */
    @PostMapping("/test/text")
    public PoetryResult<Map<String, Object>> testTranslateText(
            @RequestBody Map<String, Object> body) {

        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) body.get("config");
            String text = (String) body.get("text");
            String title = (String) body.get("title");
            String content = (String) body.get("content");

            // 从 config 获取语言信息
            String sourceLang = "zh";
            String targetLang = "en";
            if (config != null) {
                sourceLang = (String) config.getOrDefault("default_source_lang", "zh");
                targetLang = (String) config.getOrDefault("default_target_lang", "en");
            }

            // 判断测试类型：有 title+content 则为 TOON 格式文章翻译
            boolean isToonTest = (title != null && !title.isBlank() && content != null && !content.isBlank());

            if (!isToonTest && (text == null || text.isBlank())) {
                return PoetryResult.fail("翻译文本不能为空");
            }

            // 创建临时 ChatModel
            ChatModel chatModel = createChatModelFromConfig(config);
            if (chatModel == null) {
                return PoetryResult.fail("无法创建 AI 模型，请检查配置");
            }

            // 尝试从 config 中获取自定义提示词
            String customPrompt = null;
            if (config != null) {
                String type = (String) config.get("type");
                Map<String, Object> llmConfig = null;
                if ("dedicated_llm".equals(type) && config.containsKey("translation_llm")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tempLlmConfig = (Map<String, Object>) config.get("translation_llm");
                    llmConfig = tempLlmConfig;
                } else if (config.containsKey("llm")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tempLlmConfig = (Map<String, Object>) config.get("llm");
                    llmConfig = tempLlmConfig;
                }

                if (llmConfig != null) {
                    customPrompt = (String) llmConfig.get("prompt");
                }
            }

            if (isToonTest) {
                // 生成不同格式文章数据
                Map<String, Object> toonDataMap = new LinkedHashMap<>();
                toonDataMap.put("title", title);
                toonDataMap.put("content", content);
                String toonData = ToonFormatter.encode(toonDataMap);
                String jsonData = com.alibaba.fastjson.JSON.toJSONString(toonDataMap);
                String csvData = String.format("title,content\n\"%s\",\"%s\"", title.replace("\"", "\"\""),
                        content.replace("\"", "\"\""));

                String prompt;
                if (customPrompt != null && !customPrompt.isBlank()) {
                    prompt = customPrompt
                            .replace("{source_lang}", sourceLang)
                            .replace("{target_lang}", targetLang)
                            .replace("{title}", title)
                            .replace("{content}", content)
                            .replace("{toon_data}", toonData)
                            .replace("{json_data}", jsonData)
                            .replace("{csv_data}", csvData)
                            .replace("{format}", "JSON格式");
                } else {
                    prompt = String.format("""
                            请将以下文章从 %s 翻译为 %s。
                            请按以下JSON格式返回，只返回JSON，不要添加任何解释：
                            {"title":"翻译后的标题","content":"翻译后的内容"}

                            标题：%s

                            内容：
                            %s
                            """, sourceLang, targetLang, title, content);
                }

                String response = ChatClient.create(chatModel)
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();

                long elapsed = System.currentTimeMillis() - start;

                if (response != null && !response.isBlank()) {
                    // 尝试解析 JSON 响应
                    try {
                        com.alibaba.fastjson.JSONObject json = extractJsonResponse(response);
                        if (json != null) {
                            result.put("translated_title", json.getString("title"));
                            result.put("translated_content", json.getString("content"));
                        } else {
                            result.put("translated_title", "");
                            result.put("translated_content", response.trim());
                        }
                    } catch (Exception e) {
                        result.put("translated_title", "");
                        result.put("translated_content", response.trim());
                    }
                    result.put("is_toon", true);
                    result.put("engine", "llm");
                    result.put("source_lang", sourceLang);
                    result.put("target_lang", targetLang);
                    result.put("processing_time", elapsed / 1000.0);

                    // 计算 TOON 节省
                    try {
                        String toonEncoded = ToonFormatter.encode(toonDataMap);
                        String jsonStr = com.alibaba.fastjson.JSON.toJSONString(toonDataMap);
                        if (jsonStr.length() > 0) {
                            double saved = (1.0 - (double) toonEncoded.length() / jsonStr.length()) * 100;
                            result.put("token_saved_percent", String.format("%.1f", saved));
                            result.put("toon_tokens", toonEncoded.length());
                        }
                    } catch (Exception ignored) {
                    }

                    return PoetryResult.success(result);
                } else {
                    return PoetryResult.fail("翻译返回空结果");
                }

            } else {
                // 纯文本翻译测试
                String prompt;
                if (customPrompt != null && !customPrompt.isBlank()) {
                    prompt = customPrompt
                            .replace("{source_lang}", sourceLang)
                            .replace("{target_lang}", targetLang)
                            .replace("{format}", "纯文本")
                            // 纯文本翻译只有文本，直接把文本映射到 content 或者占位符里
                            .replace("{content}", text)
                            .replace("{toon_data}", text)
                            .replace("{json_data}", text)
                            .replace("{csv_data}", text)
                            .replace("{title}", "");
                } else {
                    prompt = String.format("""
                            请将以下文本从 %s 翻译为 %s。
                            只返回翻译结果，不要添加任何解释或注释。

                            原文：
                            %s
                            """, sourceLang, targetLang, text);
                }

                String response = ChatClient.create(chatModel)
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();

                long elapsed = System.currentTimeMillis() - start;

                if (response != null && !response.isBlank()) {
                    result.put("translated_text", response.trim());
                    result.put("engine", "llm");
                    result.put("source_lang", sourceLang);
                    result.put("target_lang", targetLang);
                    result.put("processing_time", elapsed / 1000.0);
                    return PoetryResult.success(result);
                } else {
                    return PoetryResult.fail("翻译返回空结果");
                }
            }

        } catch (Exception e) {
            log.error("翻译测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return PoetryResult.fail(500, "翻译异常: " + e.getMessage(), result);
        }
    }

    /**
     * 摘要生成测试（兼容前端 tempConfig 格式）
     * <p>
     * 前端调用格式:
     * POST /admin/translation/test/summary
     * Body: { config: {...}, article_id: 0, languages: {zh: "内容", en: ""},
     * max_length: 150, style: "concise" }
     */
    @PostMapping("/test/summary")
    public PoetryResult<Map<String, Object>> testGenerateSummary(
            @RequestBody Map<String, Object> body) {

        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) body.get("config");
            @SuppressWarnings("unchecked")
            Map<String, String> languages = (Map<String, String>) body.get("languages");
            int maxLength = body.containsKey("max_length")
                    ? ((Number) body.get("max_length")).intValue()
                    : 150;
            String style = (String) body.getOrDefault("style", "concise");

            if (languages == null || languages.isEmpty()) {
                return PoetryResult.fail("语言内容不能为空");
            }

            // 找到有内容的源语言
            String sourceContent = null;
            String sourceLang = null;
            for (Map.Entry<String, String> entry : languages.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isBlank()) {
                    sourceContent = entry.getValue();
                    sourceLang = entry.getKey();
                    break;
                }
            }

            if (sourceContent == null) {
                return PoetryResult.fail("请提供至少一种语言的内容");
            }

            // 创建 ChatModel
            ChatModel chatModel = createSummaryChatModelFromConfig(config);
            if (chatModel == null) {
                return PoetryResult.fail("无法创建 AI 模型，请检查配置");
            }

            // 构建摘要提示词
            StringBuilder langListBuilder = new StringBuilder();
            for (String langCode : languages.keySet()) {
                if (langListBuilder.length() > 0)
                    langListBuilder.append("、");
                langListBuilder.append(langCode);
            }

            String styleDesc = switch (style) {
                case "detailed" -> "详细全面，包含文章的主要内容和关键信息";
                case "academic" -> "学术风格，使用专业术语和结构化表达";
                case "creative" -> "创意风格，引人入胜";
                default -> "简洁明了，突出文章的核心观点";
            };

            // 生成 TOON 格式示例
            Map<String, Object> exampleSummaries = new LinkedHashMap<>();
            for (String langCode : languages.keySet()) {
                exampleSummaries.put(langCode, langCode + "摘要内容");
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

            // 尝试从 config.summary.prompt 获取自定义提示词
            String customPromptTemplate = null;
            if (config != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> summaryConfig = (Map<String, Object>) config.get("summary");
                if (summaryConfig != null) {
                    customPromptTemplate = (String) summaryConfig.get("prompt");
                }
            }

            String prompt;
            if (customPromptTemplate != null && !customPromptTemplate.isBlank()) {
                // 使用自定义提示词，支持占位符替换
                prompt = customPromptTemplate
                        .replace("{max_length}", String.valueOf(maxLength))
                        .replace("{style_desc}", styleDesc)
                        .replace("{content_text}", sourceContent)
                        .replace("{source_content}", sourceContent)
                        .replace("{languages}", langListBuilder.toString())
                        .replace("{source_lang}", sourceLang)
                        .replace("{toon_example}", toonExample)
                        .replace("{json_example}", jsonExample)
                        .replace("{csv_example}", csvExample);
            } else {
                // 默认提示词：使用 TOON 格式
                prompt = String.format("""
                        请为以下%s文章生成多语言摘要，要求：
                        1. 生成语言：%s
                        2. 风格：%s
                        3. 每个语言的摘要长度控制在%d字符以内
                        4. 保持TOON格式结构不变（2个空格缩进）
                        5. 只返回TOON格式数据，不添加任何解释或markdown代码块标记
                        6. 注意：为每个目标语言生成该语言的摘要（如需要英文摘要，则生成英文；如需要日文摘要，则生成日文）

                        文章内容（%s）：
                        %s

                        请返回TOON格式的摘要，格式如下：
                        %s""", sourceLang, langListBuilder, styleDesc, maxLength,
                        sourceLang, sourceContent, toonExample);
            }

            String response = ChatClient.create(chatModel)
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - start;

            if (response != null && !response.isBlank()) {
                Map<String, String> summaries = null;

                // 1. 先尝试 TOON 格式解析
                try {
                    Map<String, Object> toonDecoded = ToonFormatter.decode(response);
                    if (toonDecoded != null) {
                        Object summariesObj = toonDecoded.get("summaries");
                        if (summariesObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> summariesMap = (Map<String, Object>) summariesObj;
                            summaries = new LinkedHashMap<>();
                            for (Map.Entry<String, Object> e : summariesMap.entrySet()) {
                                if (e.getValue() instanceof String s) {
                                    summaries.put(e.getKey(), s);
                                }
                            }
                        }
                        if (summaries == null || summaries.isEmpty()) {
                            // 扁平结构
                            summaries = new LinkedHashMap<>();
                            for (Map.Entry<String, Object> e : toonDecoded.entrySet()) {
                                if (e.getValue() instanceof String s) {
                                    summaries.put(e.getKey(), s);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                // 2. JSON 兜底
                if (summaries == null || summaries.isEmpty()) {
                    com.alibaba.fastjson.JSONObject json = extractJsonResponse(response);
                    if (json != null) {
                        summaries = new LinkedHashMap<>();
                        for (String key : json.keySet()) {
                            summaries.put(key, json.getString(key));
                        }
                    }
                }

                // 3. 键值对 & CSV / TSV 兜底（针对缩进丢失、逗号/制表符/冒号分割的纯文本）
                if (summaries == null || summaries.isEmpty()) {
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
                            if (languages.containsKey(lang) && !summary.isBlank()) {
                                csvResult.put(lang, summary);
                            }
                        }
                    }
                    if (!csvResult.isEmpty()) {
                        summaries = csvResult;
                    }
                }

                // 4. 纯文本兜底
                if (summaries == null || summaries.isEmpty()) {
                    summaries = new LinkedHashMap<>();
                    summaries.put(sourceLang, response.trim());
                }

                result.put("success", true);
                result.put("summaries", summaries);
                result.put("method", "llm");
                result.put("processing_time", String.format("%.2f", elapsed / 1000.0));
                return PoetryResult.success(result);
            } else {
                result.put("success", false);
                result.put("error_message", "摘要生成返回空结果");
                return PoetryResult.fail(500, "摘要生成失败", result);
            }

        } catch (Exception e) {
            log.error("摘要生成测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error_message", e.getMessage());
            return PoetryResult.fail(500, "摘要生成异常: " + e.getMessage(), result);
        }
    }

    /**
     * 快速连接测试
     * <p>
     * 用极简文本测试 AI 连接是否正常
     */
    @PostMapping("/test/connection")
    public PoetryResult<Map<String, Object>> testConnection(
            @RequestBody Map<String, Object> body) {

        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) body.get("config");
            String text = (String) body.getOrDefault("text", "Hi");

            ChatModel chatModel = createChatModelFromConfig(config);
            if (chatModel == null) {
                return PoetryResult.fail("无法创建 AI 模型，请检查配置");
            }

            String response = ChatClient.create(chatModel)
                    .prompt()
                    .user("请用一句话回复：" + text)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - start;

            if (response != null && !response.isBlank()) {
                result.put("success", true);
                result.put("response", response.trim());
                result.put("elapsed_ms", elapsed);
                return PoetryResult.success(result);
            } else {
                return PoetryResult.fail("连接测试返回空响应");
            }

        } catch (Exception e) {
            log.error("连接测试失败: {}", e.getMessage(), e);
            return PoetryResult.fail("连接测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试 TOON 格式编解码
     */
    @PostMapping("/test/toon")
    public PoetryResult<Map<String, Object>> testToonFormat(
            @RequestBody Map<String, Object> body) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 编码测试
            String toonEncoded = ToonFormatter.encode(body);
            result.put("toon", toonEncoded);

            // 解码验证
            Map<String, Object> decoded = ToonFormatter.decode(toonEncoded);
            result.put("roundTrip", decoded);

            // Token 对比（粗略计算）
            String jsonStr = com.alibaba.fastjson.JSON.toJSONString(body);
            result.put("jsonLength", jsonStr.length());
            result.put("toonLength", toonEncoded.length());
            double saved = jsonStr.length() > 0
                    ? (1.0 - (double) toonEncoded.length() / jsonStr.length()) * 100
                    : 0;
            result.put("savedPercent", String.format("%.1f%%", saved));
            result.put("success", true);

            return PoetryResult.success(result);

        } catch (Exception e) {
            log.error("TOON 测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return PoetryResult.fail(500, "TOON 测试异常: " + e.getMessage(), result);
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 从前端 tempConfig 中创建翻译用 ChatModel
     * 优先使用 translation_llm（独立翻译配置），否则使用全局 llm
     */
    private ChatModel createChatModelFromConfig(Map<String, Object> config) {
        if (config == null) {
            log.error("config 对象为空");
            return null;
        }

        try {
            // 确定使用哪个 LLM 配置
            Map<String, Object> llmConfig = null;
            String type = (String) config.getOrDefault("type", "llm");

            if ("dedicated_llm".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> translationLlm = (Map<String, Object>) config.get("translation_llm");
                if (translationLlm != null) {
                    llmConfig = translationLlm;
                }
            }

            // 如果没有独立配置，使用全局 llm
            if (llmConfig == null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> globalLlm = (Map<String, Object>) config.get("llm");
                llmConfig = globalLlm;
            }

            if (llmConfig == null) {
                log.error("无法找到 LLM 配置");
                return null;
            }

            return createChatModelFromMap(llmConfig, "翻译测试");
        } catch (Exception e) {
            log.error("从 config 创建 ChatModel 失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从前端 tempConfig 中创建摘要用 ChatModel
     * 优先使用 summary.dedicated_llm，否则使用全局 llm
     */
    private ChatModel createSummaryChatModelFromConfig(Map<String, Object> config) {
        if (config == null) {
            log.error("config 对象为空");
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> summaryConfig = (Map<String, Object>) config.get("summary");

            if (summaryConfig != null) {
                String summaryMode = (String) summaryConfig.getOrDefault("summaryMode", "global");
                if ("dedicated".equals(summaryMode)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dedicatedLlm = (Map<String, Object>) summaryConfig.get("dedicated_llm");
                    if (dedicatedLlm != null) {
                        return createChatModelFromMap(dedicatedLlm, "摘要独立AI");
                    }
                }
            }

            // 使用全局 llm
            @SuppressWarnings("unchecked")
            Map<String, Object> globalLlm = (Map<String, Object>) config.get("llm");
            if (globalLlm == null) {
                log.error("无法找到全局 LLM 配置用于摘要");
                return null;
            }
            return createChatModelFromMap(globalLlm, "摘要全局AI");
        } catch (Exception e) {
            log.error("从 config 创建摘要 ChatModel 失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从 Map 配置创建 ChatModel
     * Map 格式: {model, api_url, api_key, interface_type, timeout, ...}
     */
    private ChatModel createChatModelFromMap(Map<String, Object> llmConfig, String label) {
        try {
            SysAiConfig tempConfig = new SysAiConfig();
            tempConfig.setModel((String) llmConfig.get("model"));
            tempConfig.setApiBase((String) llmConfig.get("api_url"));

            // API key 处理：前端仅在用户输入新密钥时才发送 api_key
            // 如果未发送，从数据库已保存的配置中读取
            String apiKey = (String) llmConfig.get("api_key");
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = getFallbackApiKey(llmConfig);
            }
            tempConfig.setApiKey(apiKey);

            if (tempConfig.getApiKey() == null || tempConfig.getApiKey().isBlank()) {
                log.error("无法获取 API key：前端未提供且数据库中无已保存的配置");
                return null;
            }

            // interface_type 映射到 provider
            String interfaceType = (String) llmConfig.get("interface_type");
            if (interfaceType != null) {
                tempConfig.setProvider(switch (interfaceType.toLowerCase()) {
                    case "openai", "openai_compatible" -> "openai";
                    case "anthropic" -> "anthropic";
                    default -> "openai"; // 默认 OpenAI 兼容
                });
            } else {
                tempConfig.setProvider("openai");
            }

            // 翻译/测试使用较低温度确保准确性
            tempConfig.setTemperature(new java.math.BigDecimal("0.3"));

            log.info("从 tempConfig 创建 {} ChatModel: provider={}, model={}",
                    label, tempConfig.getProvider(), tempConfig.getModel());

            return chatClientFactory.createChatModel(tempConfig);
        } catch (Exception e) {
            log.error("从 Map 创建 {} ChatModel 失败: {}", label, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从数据库已保存的 article_ai 配置中获取 API key 作为兜底
     * 根据 llmConfig 中的 api_url 匹配对应的密钥
     */
    private String getFallbackApiKey(Map<String, Object> llmConfig) {
        try {
            SysAiConfig savedConfig = sysAiConfigService.getArticleAiConfigInternal("default");
            if (savedConfig == null) {
                log.warn("数据库中无已保存的 article_ai 配置，无法兜底获取 API key");
                return null;
            }

            // 尝试从已保存配置的各级 JSON 中匹配 API key
            String requestUrl = (String) llmConfig.get("api_url");

            // 1. 尝试匹配全局 llmConfig
            String apiKey = extractApiKeyFromJson(savedConfig.getLlmConfig(), requestUrl);
            if (apiKey != null)
                return apiKey;

            // 2. 尝试匹配翻译独立 LLM 配置
            apiKey = extractApiKeyFromJson(savedConfig.getTranslationLlmConfig(), requestUrl);
            if (apiKey != null)
                return apiKey;

            // 3. 尝试匹配摘要配置中的 dedicated_llm
            if (savedConfig.getSummaryConfig() != null) {
                try {
                    com.alibaba.fastjson.JSONObject summaryJson = com.alibaba.fastjson.JSON
                            .parseObject(savedConfig.getSummaryConfig());
                    com.alibaba.fastjson.JSONObject dedicatedLlm = summaryJson.getJSONObject("dedicated_llm");
                    if (dedicatedLlm != null) {
                        String key = dedicatedLlm.getString("api_key");
                        if (key != null && !key.isBlank())
                            return key;
                    }
                } catch (Exception ignored) {
                }
            }

            // 4. 最后兜底：直接使用顶层 apiKey
            if (savedConfig.getApiKey() != null && !savedConfig.getApiKey().isBlank()) {
                log.info("使用顶层 apiKey 作为兜底");
                return savedConfig.getApiKey();
            }

            log.warn("数据库已保存配置中未找到匹配的 API key");
            return null;
        } catch (Exception e) {
            log.error("兜底获取 API key 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 JSON 配置字符串中提取 api_key
     */
    private String extractApiKeyFromJson(String jsonConfig, String requestUrl) {
        if (jsonConfig == null || jsonConfig.isBlank())
            return null;
        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(jsonConfig);
            String key = json.getString("api_key");
            if (key != null && !key.isBlank()) {
                return key;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 从 LLM 响应中提取 JSON 对象
     */
    private com.alibaba.fastjson.JSONObject extractJsonResponse(String text) {
        if (text == null)
            return null;

        // 尝试直接解析
        try {
            return com.alibaba.fastjson.JSON.parseObject(text.trim());
        } catch (Exception ignored) {
        }

        // 尝试从 markdown code block 中提取
        String cleaned = text;
        if (cleaned.contains("```json")) {
            cleaned = cleaned.substring(cleaned.indexOf("```json") + 7);
            if (cleaned.contains("```")) {
                cleaned = cleaned.substring(0, cleaned.indexOf("```"));
            }
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.substring(cleaned.indexOf("```") + 3);
            if (cleaned.contains("```")) {
                cleaned = cleaned.substring(0, cleaned.indexOf("```"));
            }
        }

        try {
            return com.alibaba.fastjson.JSON.parseObject(cleaned.trim());
        } catch (Exception ignored) {
        }

        // 尝试找到第一个 { 和最后一个 }
        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first >= 0 && last > first) {
            try {
                return com.alibaba.fastjson.JSON.parseObject(text.substring(first, last + 1));
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
