package com.ld.poetry.event;

import com.ld.poetry.service.ai.rag.RagSyncService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ArticleRagSyncListener {

    private final RagSyncService ragSyncService;

    public ArticleRagSyncListener(RagSyncService ragSyncService) {
        this.ragSyncService = ragSyncService;
    }

    @EventListener
    public void handleArticleSaved(ArticleSavedEvent event) {
        if (event == null || event.getArticleId() == null) {
            return;
        }
        ragSyncService.syncArticleAsync(event.getArticleId());
    }
}
