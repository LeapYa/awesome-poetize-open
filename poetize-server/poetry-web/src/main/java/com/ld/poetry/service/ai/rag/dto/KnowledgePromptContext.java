package com.ld.poetry.service.ai.rag.dto;

import java.util.List;

public record KnowledgePromptContext(
        String promptContext,
        List<AdminNavigationAction> actions,
        String retrievalQuery,
        List<KnowledgeSearchHit> rawHits,
        String ragVersion,
        long retrievalDurationMs) {

    private static final KnowledgePromptContext EMPTY = new KnowledgePromptContext("", List.of(), "",
            List.of(), "0", 0L);

    public static KnowledgePromptContext empty() {
        return EMPTY;
    }

    public static KnowledgePromptContext empty(String retrievalQuery, String ragVersion, long retrievalDurationMs) {
        return new KnowledgePromptContext("", List.of(), retrievalQuery != null ? retrievalQuery : "",
                List.of(), ragVersion != null ? ragVersion : "0", retrievalDurationMs);
    }
}
