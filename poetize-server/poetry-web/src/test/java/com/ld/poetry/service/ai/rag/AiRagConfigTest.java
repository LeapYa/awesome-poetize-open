package com.ld.poetry.service.ai.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiRagConfigTest {

    @Test
    void shouldExposeUnsavedEnableHintWhenRagDisabled() {
        AiRagConfig config = new AiRagConfig(
                false,
                "poetize_ai_chat",
                "openai",
                "https://api.openai.com/v1",
                "test-key",
                "text-embedding-3-small",
                5,
                0.2D,
                700,
                120,
                1536,
                "mariadb",
                "11.8.1-MariaDB",
                true,
                true,
                null);

        assertEquals("RAG 当前未启用；如果刚修改过配置，请先点击页面底部的“保存外观与排版设置”。", config.runnableBlockReason());
    }

    @Test
    void shouldExplainMissingEmbeddingKey() {
        AiRagConfig config = new AiRagConfig(
                true,
                "poetize_ai_chat",
                "openai",
                "https://api.openai.com/v1",
                null,
                "text-embedding-3-small",
                5,
                0.2D,
                700,
                120,
                1536,
                "mariadb",
                "11.8.1-MariaDB",
                true,
                true,
                null);

        assertEquals("Embedding API Key 未配置，且 AI 聊天主模型 API Key 也不可用。", config.runnableBlockReason());
    }

    @Test
    void shouldReturnNullWhenRagIsRunnable() {
        AiRagConfig config = new AiRagConfig(
                true,
                "poetize_ai_chat",
                "openai",
                "https://api.openai.com/v1",
                "test-key",
                "text-embedding-3-small",
                5,
                0.2D,
                700,
                120,
                1536,
                "mariadb",
                "11.8.1-MariaDB",
                true,
                true,
                null);

        assertNull(config.runnableBlockReason());
    }
}
