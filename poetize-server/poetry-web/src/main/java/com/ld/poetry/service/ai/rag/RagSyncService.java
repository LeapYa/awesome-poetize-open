package com.ld.poetry.service.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class RagSyncService {

    private final KnowledgeIndexService knowledgeIndexService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public RagSyncService(KnowledgeIndexService knowledgeIndexService,
            KnowledgeRetrievalService knowledgeRetrievalService) {
        this.knowledgeIndexService = knowledgeIndexService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Async
    public void syncArticleAsync(Integer articleId) {
        syncSourceAsync(KnowledgeSourceType.ARTICLE.name(), String.valueOf(articleId));
    }

    @Async
    public void rebuildAllAsync() {
        try {
            knowledgeIndexService.rebuildAll();
        } catch (Exception e) {
            log.warn("RAG 全量重建失败: {}", e.getMessage(), e);
        }
    }

    public void syncSourceAsync(String sourceType, String sourceId) {
        try {
            knowledgeIndexService.syncSource(sourceType, sourceId);
        } catch (Exception e) {
            log.warn("RAG 增量同步失败: sourceType={}, sourceId={}, error={}", sourceType, sourceId, e.getMessage(), e);
        }
    }

    public Map<String, Object> getStatus() {
        return knowledgeIndexService.getSyncStatus();
    }

    public String getBlockingReason() {
        return knowledgeIndexService.getBlockingReason();
    }

    public Map<String, Object> preview(String query, Map<String, Object> pageContext) {
        var promptContext = knowledgeRetrievalService.buildPromptContext(query, pageContext);
        return Map.of(
                "retrievalQuery", promptContext.retrievalQuery(),
                "rawHits", promptContext.rawHits(),
                "assembledContext", promptContext.promptContext(),
                "ragVersion", promptContext.ragVersion(),
                "retrievalDurationMs", promptContext.retrievalDurationMs(),
                "status", knowledgeIndexService.getSyncStatus());
    }
}
