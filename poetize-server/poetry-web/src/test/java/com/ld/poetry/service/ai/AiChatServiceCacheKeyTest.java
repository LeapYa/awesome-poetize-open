package com.ld.poetry.service.ai;

import com.ld.poetry.entity.SysAiConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AiChatServiceCacheKeyTest {

    private final AiChatService aiChatService = new AiChatService();

    @Test
    void shouldIncludeRagVersionInCacheKey() {
        SysAiConfig config = new SysAiConfig();
        config.setProvider("openai");
        config.setModel("gpt-5.4");

        String keyWithV1 = aiChatService.buildCacheKey("你好", config, "v1");
        String keyWithV2 = aiChatService.buildCacheKey("你好", config, "v2");
        String keyWithV1Again = aiChatService.buildCacheKey("你好", config, "v1");

        assertNotEquals(keyWithV1, keyWithV2);
        assertEquals(keyWithV1, keyWithV1Again);
    }
}
