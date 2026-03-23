package com.ld.poetry.service.ai.rag.dto;

import com.ld.poetry.service.ai.rag.KnowledgeVisibilityScope;

import java.time.LocalDateTime;
import java.util.Map;

public record KnowledgeSourceDocument(
        String sourceType,
        String sourceId,
        String title,
        String content,
        KnowledgeVisibilityScope visibilityScope,
        LocalDateTime updatedAt,
        Map<String, Object> metadata) {

    public String documentId() {
        return sourceType + ":" + sourceId;
    }
}
