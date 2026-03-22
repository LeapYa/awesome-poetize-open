package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.SysAiConfigMapper;
import com.ld.poetry.entity.SysAiConfig;
import com.ld.poetry.entity.Article;
import com.ld.poetry.service.SysAiConfigService;
import com.ld.poetry.service.ai.DynamicChatClientFactory;
import com.ld.poetry.utils.AESCryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI配置服务实现类
 * 
 * @author LeapYa
 * @since 2025-10-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAiConfigServiceImpl extends ServiceImpl<SysAiConfigMapper, SysAiConfig>
        implements SysAiConfigService {

    private final SysAiConfigMapper sysAiConfigMapper;
    private final ArticleMapper articleMapper;
    private final AESCryptoUtil aesCryptoUtil;
    private final DynamicChatClientFactory dynamicChatClientFactory;

    @Autowired
    @Lazy
    private com.ld.poetry.service.ai.LlmTranslationService llmTranslationService;

    // ========== 配置查询方法 ==========

    @Override
    public SysAiConfig getConfig(String configType, String configName) {
        if (!StringUtils.hasText(configName)) {
            configName = "default";
        }

        SysAiConfig config = sysAiConfigMapper.selectByTypeAndName(configType, configName);

        // 解密敏感字段并脱敏显示
        if (config != null) {
            decryptAndMaskConfig(config);
        }

        return config;
    }

    @Override
    public SysAiConfig getAiChatConfig(String configName) {
        return getConfig("ai_chat", configName);
    }

    @Override
    public Map<String, Object> getStreamingConfig(String configName) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取配置（已脱敏）
            SysAiConfig config = getAiChatConfig(configName);

            if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
                // 返回默认配置
                result.put("enabled", false);
                result.put("streaming_enabled", false);
                result.put("configured", false);
                result.put("require_login", false);
                result.put("chat_name", "AI助手");
                result.put("welcome_message", "你好！我是你的AI助手，有什么可以帮助你的吗？");
                result.put("theme_color", "#4facfe");
                result.put("enable_typing_indicator", true);
                result.put("show_timestamp", true);
                result.put("max_message_length", 500);
                result.put("max_conversation_length", 20);
                result.put("rate_limit", 20);
                return result;
            }

            // 基础配置
            result.put("enabled", config.getEnabled());
            result.put("streaming_enabled", Boolean.TRUE.equals(config.getEnableStreaming()));
            result.put("configured", StringUtils.hasText(config.getProvider())
                    && StringUtils.hasText(config.getApiKey())
                    && StringUtils.hasText(config.getModel()));

            // 聊天配置
            result.put("require_login", Boolean.TRUE.equals(config.getRequireLogin()));
            result.put("chat_name", StringUtils.hasText(config.getChatName()) ? config.getChatName() : "AI助手");
            result.put("welcome_message", StringUtils.hasText(config.getWelcomeMessage())
                    ? config.getWelcomeMessage()
                    : "你好！我是你的AI助手，有什么可以帮助你的吗？");
            result.put("theme_color", StringUtils.hasText(config.getThemeColor()) ? config.getThemeColor() : "#4facfe");
            result.put("max_message_length", config.getMaxMessageLength() != null ? config.getMaxMessageLength() : 500);
            result.put("max_conversation_length",
                    config.getMaxConversationLength() != null ? config.getMaxConversationLength() : 20);
            result.put("rate_limit", config.getRateLimit() != null ? config.getRateLimit() : 20);
            result.put("enable_content_filter", Boolean.TRUE.equals(config.getEnableContentFilter()));

            // 显示配置
            result.put("enable_typing_indicator", Boolean.TRUE.equals(config.getEnableTypingIndicator()));
            result.put("show_timestamp", !Boolean.FALSE.equals(config.getShowTimestamp()));
            result.put("enable_chat_history", Boolean.TRUE.equals(config.getEnableChatHistory()));

        } catch (Exception e) {
            log.error("获取流式响应配置失败: {}", e.getMessage(), e);
            // 返回默认配置
            result.put("enabled", false);
            result.put("streaming_enabled", false);
            result.put("configured", false);
            result.put("require_login", false);
            result.put("chat_name", "AI助手");
            result.put("welcome_message", "你好！我是你的AI助手，有什么可以帮助你的吗？");
            result.put("theme_color", "#4facfe");
            result.put("enable_typing_indicator", true);
            result.put("show_timestamp", true);
            result.put("max_message_length", 500);
            result.put("max_conversation_length", 20);
            result.put("rate_limit", 20);
        }

        return result;
    }

    @Override
    public SysAiConfig getAiChatConfigInternal(String configName) {
        return getDecryptedConfig("ai_chat", configName);
    }

    @Override
    public SysAiConfig getArticleAiConfig(String configName) {
        return getConfig("article_ai", configName);
    }

    @Override
    public SysAiConfig getArticleAiConfigInternal(String configName) {
        return getDecryptedConfig("article_ai", configName);
    }

    @Override
    public SysAiConfig getAiApiConfig(String configName) {
        return getConfig("ai_api", configName);
    }

    // ========== 配置保存方法 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateConfig(SysAiConfig config) {
        if (config == null || !StringUtils.hasText(config.getConfigType())) {
            log.error("保存配置失败：配置对象或配置类型为空");
            return false;
        }

        try {
            // 设置默认配置名称
            if (!StringUtils.hasText(config.getConfigName())) {
                config.setConfigName("default");
            }

            // 加密敏感字段
            encryptSensitiveFields(config);

            // 检查是否已存在
            SysAiConfig existingConfig = sysAiConfigMapper.selectByTypeAndName(
                    config.getConfigType(), config.getConfigName());

            boolean success;
            if (existingConfig != null) {
                // 更新现有配置
                config.setId(existingConfig.getId());
                success = updateById(config);
                log.info("更新AI配置成功: type={}, name={}", config.getConfigType(), config.getConfigName());
            } else {
                // 插入新配置
                success = save(config);
                log.info("插入AI配置成功: type={}, name={}", config.getConfigType(), config.getConfigName());
            }

            return success;

        } catch (Exception e) {
            log.error("保存AI配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存AI配置失败: " + e.getMessage());
        }
    }

    @Override
    public boolean saveAiChatConfig(SysAiConfig config) {
        config.setConfigType("ai_chat");
        return saveOrUpdateConfig(config);
    }

    @Override
    public boolean saveArticleAiConfig(SysAiConfig config) {
        config.setConfigType("article_ai");

        // 业务逻辑验证：如果系统中已有文章，不允许修改源语言
        SysAiConfig existingConfig = sysAiConfigMapper.selectByTypeAndName("article_ai",
                config.getConfigName() != null ? config.getConfigName() : "default");
        if (existingConfig != null && existingConfig.getDefaultSourceLang() != null) {
            // 检查是否修改了源语言
            if (config.getDefaultSourceLang() != null
                    && !config.getDefaultSourceLang().equals(existingConfig.getDefaultSourceLang())) {
                // 检查系统中是否已有文章
                if (hasArticles()) {
                    log.warn("系统中已有文章数据，不允许修改源语言从 {} 到 {}", existingConfig.getDefaultSourceLang(),
                            config.getDefaultSourceLang());
                    throw new RuntimeException("系统中已有文章数据，不允许修改源语言配置。修改源语言会导致现有文章的语言标识混乱，影响SEO和翻译关系。");
                }
            }
        }

        return saveOrUpdateConfig(config);
    }

    /**
     * 检查系统中是否已有文章数据
     */
    @Override
    public boolean hasArticles() {
        try {
            // 查询文章表，检查是否有数据
            Long count = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                    .last("LIMIT 1"));
            boolean hasArticles = count != null && count > 0;
            log.info("检查文章数据结果: 总数={}, 有文章={}", count, hasArticles);
            return hasArticles;
        } catch (Exception e) {
            // 查询失败时为了安全起见，假设有文章数据，阻止修改源语言
            log.error("检查文章数据失败，默认认为有文章: {}", e.getMessage());
            return true;
        }
    }

    @Override
    public boolean saveAiApiConfig(SysAiConfig config) {
        config.setConfigType("ai_api");
        return saveOrUpdateConfig(config);
    }

    // ========== 其他功能方法 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleEnabled(Integer id) {
        if (id == null) {
            return false;
        }

        try {
            SysAiConfig config = getById(id);
            if (config == null) {
                log.error("配置不存在: id={}", id);
                return false;
            }

            boolean newEnabled = !Boolean.TRUE.equals(config.getEnabled());
            int rows = sysAiConfigMapper.updateEnabled(id, newEnabled);

            log.info("切换配置启用状态成功: id={}, enabled={}", id, newEnabled);
            return rows > 0;

        } catch (Exception e) {
            log.error("切换配置启用状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> testConnection(SysAiConfig config) {
        Map<String, Object> result = new HashMap<>();

        if (config == null) {
            result.put("success", false);
            result.put("message", "配置对象为空");
            return result;
        }

        try {
            String configType = config.getConfigType();

            if ("ai_chat".equals(configType) || "ai_api".equals(configType)) {
                return testAiApiConnection(config);
            } else if ("translation".equals(configType)) {
                return testTranslationConnection(config);
            } else {
                result.put("success", false);
                result.put("message", "不支持的配置类型: " + configType);
                return result;
            }

        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试连接失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public List<SysAiConfig> listAllConfigs() {
        List<SysAiConfig> configs = list(new LambdaQueryWrapper<SysAiConfig>()
                .orderByAsc(SysAiConfig::getConfigType)
                .orderByAsc(SysAiConfig::getId));

        // 解密并脱敏所有配置
        configs.forEach(this::decryptAndMaskConfig);

        return configs;
    }

    @Override
    public List<SysAiConfig> listConfigsByType(String configType) {
        List<SysAiConfig> configs = sysAiConfigMapper.selectByType(configType);

        // 解密并脱敏所有配置
        configs.forEach(this::decryptAndMaskConfig);

        return configs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Integer id) {
        if (id == null) {
            return false;
        }

        try {
            boolean success = removeById(id);
            log.info("删除AI配置成功: id={}", id);
            return success;

        } catch (Exception e) {
            log.error("删除AI配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getDefaultLanguages() {
        Map<String, Object> result = new HashMap<>();

        // 只需要语言配置，直接查询数据库，无需解密敏感字段
        SysAiConfig config = sysAiConfigMapper.selectByTypeAndName("article_ai", "default");

        if (config != null) {
            result.put("default_source_lang",
                    config.getDefaultSourceLang() != null ? config.getDefaultSourceLang() : "zh");
            result.put("default_target_lang",
                    config.getDefaultTargetLang() != null ? config.getDefaultTargetLang() : "en");
        } else {
            // 配置不存在，返回默认值
            result.put("default_source_lang", "zh");
            result.put("default_target_lang", "en");
        }

        return result;
    }

    @Override
    public Map<String, String> getLanguageMapping() {
        // 前台展示用语言映射（原生语言文字）
        Map<String, String> mapping = new HashMap<>();
        mapping.put("zh", "中文");
        mapping.put("zh-TW", "繁體中文");
        mapping.put("en", "English");
        mapping.put("ja", "日本語");
        mapping.put("ko", "한국어");
        mapping.put("fr", "Français");
        mapping.put("de", "Deutsch");
        mapping.put("es", "Español");
        mapping.put("ru", "Русский");
        mapping.put("pt", "Português");
        mapping.put("it", "Italiano");
        mapping.put("ar", "العربية");
        mapping.put("th", "ไทย");
        mapping.put("vi", "Tiếng Việt");
        mapping.put("auto", "Auto Detect");
        return mapping;
    }

    @Override
    public Map<String, String> getLanguageMappingAdmin() {
        // 后台管理用语言映射（中文）
        Map<String, String> mapping = new HashMap<>();
        mapping.put("zh", "中文");
        mapping.put("zh-TW", "繁体中文");
        mapping.put("en", "英文");
        mapping.put("ja", "日文");
        mapping.put("ko", "韩文");
        mapping.put("fr", "法文");
        mapping.put("de", "德文");
        mapping.put("es", "西班牙文");
        mapping.put("ru", "俄文");
        mapping.put("pt", "葡萄牙文");
        mapping.put("it", "意大利文");
        mapping.put("ar", "阿拉伯文");
        mapping.put("th", "泰文");
        mapping.put("vi", "越南文");
        mapping.put("auto", "自动检测");
        return mapping;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 加密敏感字段
     *
     * @param config AI配置对象
     */
    private void encryptSensitiveFields(SysAiConfig config) {
        // 加密API密钥
        if (StringUtils.hasText(config.getApiKey())) {
            String encrypted = aesCryptoUtil.encrypt(config.getApiKey());
            config.setApiKey(encrypted);
        }

        // 加密Mem0 API密钥
        if (StringUtils.hasText(config.getMem0ApiKey())) {
            String encrypted = aesCryptoUtil.encrypt(config.getMem0ApiKey());
            config.setMem0ApiKey(encrypted);
        }

        // 加密JSON字段中的敏感信息
        encryptJsonFields(config);
    }

    /**
     * 加密JSON字段中的敏感信息
     */
    private void encryptJsonFields(SysAiConfig config) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // 1. 加密百度翻译配置中的app_secret
            if (StringUtils.hasText(config.getBaiduConfig())) {
                JsonNode baiduNode = objectMapper.readTree(config.getBaiduConfig());
                if (baiduNode.has("app_secret")) {
                    String appSecret = baiduNode.get("app_secret").asText();
                    if (StringUtils.hasText(appSecret) && !appSecret.startsWith("ENC(")) {
                        ((ObjectNode) baiduNode).put("app_secret", aesCryptoUtil.encrypt(appSecret));
                        config.setBaiduConfig(objectMapper.writeValueAsString(baiduNode));
                    }
                }
            }

            // 2. 加密自定义API配置中的api_key和app_secret
            if (StringUtils.hasText(config.getCustomConfig())) {
                JsonNode customNode = objectMapper.readTree(config.getCustomConfig());
                boolean modified = false;

                if (customNode.has("api_key")) {
                    String apiKey = customNode.get("api_key").asText();
                    if (StringUtils.hasText(apiKey) && !apiKey.startsWith("ENC(")) {
                        ((ObjectNode) customNode).put("api_key", aesCryptoUtil.encrypt(apiKey));
                        modified = true;
                    }
                }

                if (customNode.has("app_secret")) {
                    String appSecret = customNode.get("app_secret").asText();
                    if (StringUtils.hasText(appSecret) && !appSecret.startsWith("ENC(")) {
                        ((ObjectNode) customNode).put("app_secret", aesCryptoUtil.encrypt(appSecret));
                        modified = true;
                    }
                }

                if (modified) {
                    config.setCustomConfig(objectMapper.writeValueAsString(customNode));
                }
            }

            // 3. 加密LLM配置中的api_key
            if (StringUtils.hasText(config.getLlmConfig())) {
                JsonNode llmNode = objectMapper.readTree(config.getLlmConfig());
                if (llmNode.has("api_key")) {
                    String apiKey = llmNode.get("api_key").asText();
                    if (StringUtils.hasText(apiKey) && !apiKey.startsWith("ENC(")) {
                        ((ObjectNode) llmNode).put("api_key", aesCryptoUtil.encrypt(apiKey));
                        config.setLlmConfig(objectMapper.writeValueAsString(llmNode));
                    }
                }
            }

            // 4. 加密摘要配置中的dedicated_llm.api_key
            if (StringUtils.hasText(config.getSummaryConfig())) {
                JsonNode summaryNode = objectMapper.readTree(config.getSummaryConfig());

                if (summaryNode.has("dedicated_llm")) {
                    JsonNode dedicatedLlmNode = summaryNode.get("dedicated_llm");

                    if (dedicatedLlmNode.has("api_key")) {
                        String apiKey = dedicatedLlmNode.get("api_key").asText();
                        if (StringUtils.hasText(apiKey) && !apiKey.startsWith("ENC(")) {
                            ((ObjectNode) dedicatedLlmNode).put("api_key", aesCryptoUtil.encrypt(apiKey));
                            config.setSummaryConfig(objectMapper.writeValueAsString(summaryNode));
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("加密JSON字段失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密配置失败: " + e.getMessage());
        }
    }

    /**
     * 解密敏感字段并脱敏显示
     *
     * @param config AI配置对象
     */
    private void decryptAndMaskConfig(SysAiConfig config) {
        // 解密并脱敏API密钥
        if (StringUtils.hasText(config.getApiKey())) {
            String decrypted = aesCryptoUtil.decrypt(config.getApiKey());
            if (decrypted != null) {
                config.setApiKey(aesCryptoUtil.mask(decrypted));
            }
        }

        // 解密并脱敏Mem0 API密钥
        if (StringUtils.hasText(config.getMem0ApiKey())) {
            String decrypted = aesCryptoUtil.decrypt(config.getMem0ApiKey());
            if (decrypted != null) {
                config.setMem0ApiKey(aesCryptoUtil.mask(decrypted));
            }
        }

        // 解密并脱敏JSON字段中的敏感信息
        decryptAndMaskJsonFields(config);
    }

    /**
     * 解密并脱敏JSON字段中的敏感信息（用于前端显示）
     */
    private void decryptAndMaskJsonFields(SysAiConfig config) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // 1. 解密并脱敏百度配置
            if (StringUtils.hasText(config.getBaiduConfig())) {
                JsonNode baiduNode = objectMapper.readTree(config.getBaiduConfig());
                if (baiduNode.has("app_secret")) {
                    String encrypted = baiduNode.get("app_secret").asText();
                    if (StringUtils.hasText(encrypted)) {
                        String decrypted = aesCryptoUtil.decrypt(encrypted);
                        if (decrypted != null) {
                            // 前端通过检查是否有值来显示"已有密钥"提示
                            ((ObjectNode) baiduNode).put("app_secret", "***");
                        }
                        config.setBaiduConfig(objectMapper.writeValueAsString(baiduNode));
                    }
                }
            }

            // 2. 解密并脱敏自定义API配置
            if (StringUtils.hasText(config.getCustomConfig())) {
                JsonNode customNode = objectMapper.readTree(config.getCustomConfig());
                boolean modified = false;

                if (customNode.has("api_key")) {
                    String encrypted = customNode.get("api_key").asText();
                    if (StringUtils.hasText(encrypted)) {
                        String decrypted = aesCryptoUtil.decrypt(encrypted);
                        if (decrypted != null) {
                            ((ObjectNode) customNode).put("api_key", "***");
                            modified = true;
                        }
                    }
                }

                if (customNode.has("app_secret")) {
                    String encrypted = customNode.get("app_secret").asText();
                    if (StringUtils.hasText(encrypted)) {
                        String decrypted = aesCryptoUtil.decrypt(encrypted);
                        if (decrypted != null) {
                            ((ObjectNode) customNode).put("app_secret", "***");
                            modified = true;
                        }
                    }
                }

                if (modified) {
                    config.setCustomConfig(objectMapper.writeValueAsString(customNode));
                }
            }

            // 3. 解密并脱敏LLM配置
            if (StringUtils.hasText(config.getLlmConfig())) {
                JsonNode llmNode = objectMapper.readTree(config.getLlmConfig());
                if (llmNode.has("api_key")) {
                    String encrypted = llmNode.get("api_key").asText();
                    if (StringUtils.hasText(encrypted)) {
                        String decrypted = aesCryptoUtil.decrypt(encrypted);
                        if (decrypted != null) {
                            ((ObjectNode) llmNode).put("api_key", "***");
                            config.setLlmConfig(objectMapper.writeValueAsString(llmNode));
                        }
                    }
                }
            }

            // 4. 解密并脱敏摘要配置中的dedicated_llm
            if (StringUtils.hasText(config.getSummaryConfig())) {
                JsonNode summaryNode = objectMapper.readTree(config.getSummaryConfig());

                if (summaryNode.has("dedicated_llm")) {
                    JsonNode dedicatedLlmNode = summaryNode.get("dedicated_llm");

                    if (dedicatedLlmNode.has("api_key")) {
                        String encrypted = dedicatedLlmNode.get("api_key").asText();
                        if (StringUtils.hasText(encrypted)) {
                            String decrypted = aesCryptoUtil.decrypt(encrypted);
                            if (decrypted != null) {
                                ((ObjectNode) dedicatedLlmNode).put("api_key", "***");
                                config.setSummaryConfig(objectMapper.writeValueAsString(summaryNode));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("解密并脱敏JSON字段失败: {}", e.getMessage(), e);
            // 不抛异常，避免影响配置读取
        }
    }

    /**
     * 获取解密后的配置（用于内部调用，不脱敏）
     *
     * @param configType 配置类型
     * @param configName 配置名称
     * @return 解密后的配置
     */
    private SysAiConfig getDecryptedConfig(String configType, String configName) {
        SysAiConfig config = sysAiConfigMapper.selectByTypeAndName(configType, configName);

        if (config != null) {
            // 仅解密，不脱敏（用于内部调用）
            if (StringUtils.hasText(config.getApiKey())) {
                String decrypted = aesCryptoUtil.decrypt(config.getApiKey());
                config.setApiKey(decrypted);
            }

            if (StringUtils.hasText(config.getMem0ApiKey())) {
                String decrypted = aesCryptoUtil.decrypt(config.getMem0ApiKey());
                config.setMem0ApiKey(decrypted);
            }

            // 解密JSON字段（不脱敏，供Python服务使用）
            decryptJsonFieldsForInternal(config);
        }

        return config;
    }

    /**
     * 解密JSON字段中的敏感信息（用于内部调用，完整返回）
     */
    private void decryptJsonFieldsForInternal(SysAiConfig config) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // 1. 解密百度配置
            if (StringUtils.hasText(config.getBaiduConfig())) {
                JsonNode baiduNode = objectMapper.readTree(config.getBaiduConfig());
                if (baiduNode.has("app_secret")) {
                    String encrypted = baiduNode.get("app_secret").asText();
                    if (StringUtils.hasText(encrypted)) {
                        String decrypted = aesCryptoUtil.decrypt(encrypted);
                        if (decrypted != null) {
                            ((ObjectNode) baiduNode).put("app_secret", decrypted);
                            config.setBaiduConfig(objectMapper.writeValueAsString(baiduNode));
                        }
                    }
                }
            }

            // 2. 解密自定义API配置
            if (StringUtils.hasText(config.getCustomConfig())) {
                JsonNode customNode = objectMapper.readTree(config.getCustomConfig());
                boolean modified = false;

                if (customNode.has("api_key")) {
                    String encrypted = customNode.get("api_key").asText();
                    if (StringUtils.hasText(encrypted)) {
                        String decrypted = aesCryptoUtil.decrypt(encrypted);
                        if (decrypted != null) {
                            ((ObjectNode) customNode).put("api_key", decrypted);
                            modified = true;
                        }
                    }
                }

                if (customNode.has("app_secret")) {
                    String encrypted = customNode.get("app_secret").asText();
                    if (StringUtils.hasText(encrypted)) {
                        String decrypted = aesCryptoUtil.decrypt(encrypted);
                        if (decrypted != null) {
                            ((ObjectNode) customNode).put("app_secret", decrypted);
                            modified = true;
                        }
                    }
                }

                if (modified) {
                    config.setCustomConfig(objectMapper.writeValueAsString(customNode));
                }
            }

            // 3. 解密LLM配置
            if (StringUtils.hasText(config.getLlmConfig())) {
                JsonNode llmNode = objectMapper.readTree(config.getLlmConfig());
                if (llmNode.has("api_key")) {
                    String encrypted = llmNode.get("api_key").asText();
                    if (StringUtils.hasText(encrypted)) {
                        String decrypted = aesCryptoUtil.decrypt(encrypted);
                        if (decrypted != null) {
                            ((ObjectNode) llmNode).put("api_key", decrypted);
                            config.setLlmConfig(objectMapper.writeValueAsString(llmNode));
                        }
                    }
                }
            }

            // 4. 解密摘要配置中的dedicated_llm
            if (StringUtils.hasText(config.getSummaryConfig())) {
                JsonNode summaryNode = objectMapper.readTree(config.getSummaryConfig());

                if (summaryNode.has("dedicated_llm")) {
                    JsonNode dedicatedLlmNode = summaryNode.get("dedicated_llm");

                    if (dedicatedLlmNode.has("api_key")) {
                        String encrypted = dedicatedLlmNode.get("api_key").asText();
                        if (StringUtils.hasText(encrypted)) {
                            String decrypted = aesCryptoUtil.decrypt(encrypted);
                            if (decrypted != null) {
                                ((ObjectNode) dedicatedLlmNode).put("api_key", decrypted);
                                config.setSummaryConfig(objectMapper.writeValueAsString(summaryNode));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("解密JSON字段失败: {}", e.getMessage(), e);
            // 不抛异常，避免影响配置读取
        }
    }

    /**
     * 测试AI API连接
     *
     * @param config AI配置
     * @return 测试结果
     */
    private Map<String, Object> testAiApiConnection(SysAiConfig config) {
        Map<String, Object> result = new HashMap<>();

        try {
            String apiKey = config.getApiKey();

            if (!StringUtils.hasText(apiKey)) {
                result.put("success", false);
                result.put("message", "API密钥为空");
                return result;
            }

            ChatModel chatModel = dynamicChatClientFactory.createChatModel(config);
            String response = ChatClient.create(chatModel)
                    .prompt()
                    .user("请只回复：ok")
                    .call()
                    .content();

            if (StringUtils.hasText(response)) {
                result.put("success", true);
                result.put("message", "连接成功");
                result.put("response", response.trim());
            } else {
                result.put("success", false);
                result.put("message", "连接测试返回空响应");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 测试翻译服务连接
     *
     * @param config 翻译配置
     * @return 测试结果
     */
    private Map<String, Object> testTranslationConnection(SysAiConfig config) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 直接使用 Java 翻译服务进行测试
            String testText = "Hello";
            String translated = llmTranslationService.translateText(testText, "en", "zh");

            if (translated != null && !translated.isBlank()) {
                result.put("success", true);
                result.put("message", "翻译连接测试成功");
                result.put("testResult", translated);
            } else {
                result.put("success", false);
                result.put("message", "翻译测试返回空结果，请检查AI配置");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "翻译测试失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> convertConfigToMap(SysAiConfig config) {
        Map<String, Object> result = new HashMap<>();

        if (config == null) {
            return result;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // 基本字段
            result.put("id", config.getId());
            result.put("configType", config.getConfigType());
            result.put("configName", config.getConfigName());
            result.put("enabled", config.getEnabled());
            result.put("translationType", config.getTranslationType());
            result.put("defaultSourceLang", config.getDefaultSourceLang());
            result.put("defaultTargetLang", config.getDefaultTargetLang());

            // 将JSON字符串字段解析为对象
            if (StringUtils.hasText(config.getBaiduConfig())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> baiduConfig = objectMapper.readValue(config.getBaiduConfig(), Map.class);
                result.put("baiduConfig", baiduConfig);
            }

            if (StringUtils.hasText(config.getCustomConfig())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> customConfig = objectMapper.readValue(config.getCustomConfig(), Map.class);
                result.put("customConfig", customConfig);
            }

            if (StringUtils.hasText(config.getLlmConfig())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> llmConfig = objectMapper.readValue(config.getLlmConfig(), Map.class);
                result.put("llmConfig", llmConfig);
            }

            if (StringUtils.hasText(config.getTranslationLlmConfig())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> translationLlmConfig = objectMapper.readValue(config.getTranslationLlmConfig(),
                        Map.class);
                result.put("translationLlmConfig", translationLlmConfig);
            }

            if (StringUtils.hasText(config.getSummaryConfig())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> summaryConfig = objectMapper.readValue(config.getSummaryConfig(), Map.class);
                result.put("summaryConfig", summaryConfig);
            }

        } catch (Exception e) {
            log.error("转换配置为Map失败: {}", e.getMessage(), e);
            // 出错时至少返回基本字段
        }

        return result;
    }
}
