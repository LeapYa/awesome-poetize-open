package com.ld.poetry.service.ai.rag;

import com.ld.poetry.dao.AiKnowledgeDocumentMapper;
import com.ld.poetry.entity.AiKnowledgeDocument;
import com.ld.poetry.service.ai.ContentSanitizer;
import com.ld.poetry.service.ai.rag.dto.KnowledgePromptContext;
import com.ld.poetry.service.ai.rag.dto.KnowledgeSearchHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiEmbeddingModel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeRetrievalServiceTest {

    @Mock
    private AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    @Mock
    private RagRuntimeFactory ragRuntimeFactory;

    @Mock
    private RagVersionService ragVersionService;

    @Mock
    private ContentSanitizer contentSanitizer;

    @Mock
    private OpenAiEmbeddingModel embeddingModel;

    private KnowledgeRetrievalService knowledgeRetrievalService;

    private AiRagConfig config;

    @BeforeEach
    void setUp() {
        knowledgeRetrievalService = new KnowledgeRetrievalService(
                knowledgeDocumentMapper,
                ragRuntimeFactory,
                ragVersionService,
                contentSanitizer);
        config = new AiRagConfig(
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
                "11.7.1-MariaDB",
                true,
                true,
                null);
    }

    @Test
    void shouldBuildRetrievalQueryWithoutPageContent() {
        Map<String, Object> pageContext = Map.of(
                "title", "RAG 文章标题",
                "type", "article",
                "content", "这里是很长的正文，不应该进入 retrieval query");
        when(contentSanitizer.sanitizePageContext(pageContext)).thenReturn(pageContext);

        String retrievalQuery = knowledgeRetrievalService.buildRetrievalQuery("请总结这篇文章", pageContext);

        assertTrue(retrievalQuery.contains("请总结这篇文章"));
        assertTrue(retrievalQuery.contains("页面标题：RAG 文章标题"));
        assertTrue(retrievalQuery.contains("页面类型：article"));
        assertFalse(retrievalQuery.contains("这里是很长的正文"));
    }

    @Test
    void shouldDeduplicateHitsMergeNeighborsAndRespectBudget() {
        Map<String, Object> pageContext = Map.of(
                "title", "RAG 强化说明",
                "type", "article",
                "content", "页面正文不会进入检索 query");
        when(ragRuntimeFactory.getCurrentConfig()).thenReturn(config);
        when(ragRuntimeFactory.createEmbeddingModel(config)).thenReturn(embeddingModel);
        when(ragVersionService.getVersion(config.indexName())).thenReturn("v1");
        when(embeddingModel.embed(anyString())).thenReturn(new float[] { 1.0F, 0.0F });
        when(contentSanitizer.sanitizePageContext(pageContext)).thenReturn(pageContext);

        List<KnowledgeSearchHit> rawHits = List.of(
                hit("doc-1", "101", "第一篇", repeat("A", 800), 2, 5, 0.96),
                hit("doc-1", "101", "第一篇", repeat("A", 800), 2, 5, 0.96),
                hit("doc-1", "101", "第一篇", repeat("B", 400), 3, 5, 0.94),
                hit("doc-2", "102", "第二篇", repeat("C", 900), 0, 3, 0.91),
                hit("doc-3", "103", "第三篇", repeat("D", 900), 1, 3, 0.88),
                hit("doc-4", "104", "第四篇", repeat("E", 900), 1, 3, 0.86));
        when(knowledgeDocumentMapper.searchSimilarChunks(
                eq(config.indexName()),
                anyString(),
                eq(KnowledgeVisibilityScope.PUBLIC_ARTICLE.name()),
                eq(12),
                eq(config.distanceThreshold()))).thenReturn(rawHits);
        when(knowledgeDocumentMapper.selectChunksByDocumentRange(config.indexName(), "doc-1", 1, 3))
                .thenReturn(List.of(
                        chunk(1, repeat("第一篇-前", 120)),
                        chunk(2, repeat("第一篇-中", 120)),
                        chunk(3, repeat("第一篇-后", 120))));
        when(knowledgeDocumentMapper.selectChunksByDocumentRange(config.indexName(), "doc-2", 0, 1))
                .thenReturn(List.of(
                        chunk(0, repeat("第二篇-片段", 220)),
                        chunk(1, repeat("第二篇-补充", 220))));

        KnowledgePromptContext promptContext = knowledgeRetrievalService.buildPromptContext("帮我总结相关文章", pageContext);

        assertEquals("v1", promptContext.ragVersion());
        assertEquals(5, promptContext.rawHits().size());
        assertTrue(promptContext.promptContext().contains("文章：第一篇"));
        assertTrue(promptContext.promptContext().contains("文章：第二篇"));
        assertFalse(promptContext.promptContext().contains("文章：第四篇"));
        assertFalse(promptContext.promptContext().contains("文章：第三篇"));
        assertNotEquals(promptContext.retrievalQuery().indexOf("页面标题：RAG 强化说明"), -1);
        assertTrue(promptContext.promptContext().contains("请优先基于以上检索事实回答"));

        ArgumentCaptor<String> vectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(knowledgeDocumentMapper).searchSimilarChunks(
                eq(config.indexName()),
                vectorCaptor.capture(),
                eq(KnowledgeVisibilityScope.PUBLIC_ARTICLE.name()),
                eq(12),
                eq(config.distanceThreshold()));
        assertEquals("[1.0,0.0]", vectorCaptor.getValue());
        verify(knowledgeDocumentMapper).selectChunksByDocumentRange(config.indexName(), "doc-1", 1, 3);
        verify(knowledgeDocumentMapper).selectChunksByDocumentRange(config.indexName(), "doc-2", 0, 1);
    }

    private KnowledgeSearchHit hit(String documentId, String sourceId, String title,
            String content, int chunkIndex, int chunkCount, double similarity) {
        KnowledgeSearchHit hit = new KnowledgeSearchHit();
        hit.setDocumentId(documentId);
        hit.setSourceType(KnowledgeSourceType.ARTICLE.name());
        hit.setSourceId(sourceId);
        hit.setTitle(title);
        hit.setContent(content);
        hit.setVisibilityScope(KnowledgeVisibilityScope.PUBLIC_ARTICLE.name());
        hit.setChunkIndex(chunkIndex);
        hit.setChunkCount(chunkCount);
        hit.setSimilarity(similarity);
        return hit;
    }

    private AiKnowledgeDocument chunk(int chunkIndex, String content) {
        AiKnowledgeDocument chunk = new AiKnowledgeDocument();
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        return chunk;
    }

    private String repeat(String value, int times) {
        return value.repeat(times);
    }
}
