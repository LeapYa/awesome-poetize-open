package com.ld.poetry.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiApiBaseUrlNormalizerTest {

    @Test
    void shouldStripTrailingV1ForOpenAiCompatibleBaseUrl() {
        assertEquals("https://api.siliconflow.cn",
                AiApiBaseUrlNormalizer.normalizeOpenAiCompatibleBaseUrl(
                        "https://api.siliconflow.cn/v1", "https://api.openai.com"));
    }

    @Test
    void shouldStripFullEmbeddingsEndpoint() {
        assertEquals("https://api.example.com",
                AiApiBaseUrlNormalizer.normalizeOpenAiCompatibleBaseUrl(
                        "https://api.example.com/v1/embeddings", "https://api.openai.com"));
    }
}
