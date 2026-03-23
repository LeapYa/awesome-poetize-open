package com.ld.poetry.service.ai;

import com.ld.poetry.entity.SysAiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

/**
 * 动态 ChatModel 工厂
 * 根据数据库 SysAiConfig 配置动态创建不同 Provider 的 ChatModel
 * 
 * 注意：传入的 config 应该是 Internal 版本（apiKey 已解密）
 * 建议通过 SysAiConfigService.getAiChatConfigInternal() 获取
 */
@Service
public class DynamicChatClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(DynamicChatClientFactory.class);

    /**
     * 根据 AI 配置创建对应的 ChatModel
     * 
     * TODO [架构问题] 当前仅从 SysAiConfig 顶层字段读取 provider/apiKey/apiBase/model，
     * 适用于 ai_chat 配置。article_ai 配置的模型信息存在 llmConfig JSON 字段中，
     * 如需支持 article_ai，需额外解析 llmConfig。迁移时应统一存储方式。
     * 
     * @param config AI 配置（apiKey 会在内部尝试 AES 解密）
     * @return ChatModel 实例
     */
    public ChatModel createChatModel(SysAiConfig config) {
        String provider = config.getProvider();
        if (provider == null || provider.isBlank()) {
            provider = "openai";
        }

        logger.debug("创建 ChatModel: provider={}, model={}, baseUrl={}",
                provider, config.getModel(), config.getApiBase());

        return switch (provider.toLowerCase()) {
            case "openai", "deepseek", "siliconflow", "custom" -> createOpenAiCompatible(config);
            case "anthropic" -> createAnthropic(config);
            default -> {
                logger.warn("不支持的 provider: {}, 降级为 OpenAI 兼容模式", provider);
                yield createOpenAiCompatible(config);
            }
        };
    }

    /**
     * 创建 OpenAI 兼容的 ChatModel（覆盖 OpenAI/DeepSeek/硅基流动/自定义）
     */
    private ChatModel createOpenAiCompatible(SysAiConfig config) {
        String apiKey = config.getApiKey(); // 已解密
        String baseUrl = AiApiBaseUrlNormalizer.normalizeOpenAiCompatibleBaseUrl(
                config.getApiBase(), "https://api.openai.com");
        String model = config.getModel() != null ? config.getModel() : "gpt-4o-mini";
        double temperature = config.getTemperature() != null ? config.getTemperature().doubleValue() : 0.7;

        var openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        var options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * 创建 Anthropic Claude ChatModel
     */
    private ChatModel createAnthropic(SysAiConfig config) {
        String apiKey = config.getApiKey(); // 已解密
        String baseUrl = normalizeAnthropicBaseUrl(config.getApiBase(), "https://api.anthropic.com");
        String model = config.getModel() != null ? config.getModel() : "claude-sonnet-4-20250514";
        double temperature = config.getTemperature() != null ? config.getTemperature().doubleValue() : 0.7;

        var anthropicApi = AnthropicApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        var options = AnthropicChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(options)
                .build();
    }

    private String normalizeAnthropicBaseUrl(String url, String defaultUrl) {
        if (url == null || url.isBlank()) {
            return defaultUrl;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
