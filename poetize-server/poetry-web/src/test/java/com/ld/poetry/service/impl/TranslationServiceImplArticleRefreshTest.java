package com.ld.poetry.service.impl;

import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.ArticleTranslationMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.ArticleTranslation;
import com.ld.poetry.event.ArticleSavedEvent;
import com.ld.poetry.service.CacheService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 校验翻译表变更后必须同时清理文章缓存 + 发布文章更新事件，
 * 否则文章页与 /article/{lang}/{id} 预渲染静态页会停留在旧内容。
 */
class TranslationServiceImplArticleRefreshTest {

    private static final Integer ARTICLE_ID = 42;

    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final ArticleTranslationMapper articleTranslationMapper = mock(ArticleTranslationMapper.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private TranslationServiceImpl newService() {
        TranslationServiceImpl service = new TranslationServiceImpl();
        ReflectionTestUtils.setField(service, "articleMapper", articleMapper);
        ReflectionTestUtils.setField(service, "articleTranslationMapper", articleTranslationMapper);
        ReflectionTestUtils.setField(service, "cacheService", cacheService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        return service;
    }

    private Article visibleArticle() {
        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setSortId(7);
        article.setLabelId(9);
        article.setViewStatus(Boolean.TRUE);
        article.setArticleSlug("Hello-World");
        return article;
    }

    private ArticleSavedEvent captureEvent() {
        ArgumentCaptor<ArticleSavedEvent> captor = ArgumentCaptor.forClass(ArticleSavedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void saveManualTranslation_evictsCacheAndPublishesArticleUpdateEvent() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(visibleArticle());
        when(articleTranslationMapper.selectOne(any())).thenReturn(null);
        when(articleTranslationMapper.insert(any(ArticleTranslation.class))).thenReturn(1);

        Map<String, Object> result = newService().saveManualTranslation(
                ARTICLE_ID, "en", "Translated title", "Translated body", null);

        assertTrue(Boolean.TRUE.equals(result.get("success")));
        verify(cacheService).evictArticleRelatedCache(ARTICLE_ID);

        ArticleSavedEvent event = captureEvent();
        assertNotNull(event);
        assertEquals(ARTICLE_ID, event.getArticleId());
        assertEquals(7, event.getSortId());
        assertEquals(9, event.getLabelId());
        assertEquals("UPDATE", event.getOperationType());
        assertTrue(Boolean.TRUE.equals(event.getViewStatus()));
        // 预渲染清理依赖该 slug 一并删除 article/{slug} 目录（含各语言静态页）
        assertEquals("Hello-World", event.getPreviousArticleSlug());
    }

    @Test
    void deleteSpecificTranslation_evictsCacheAndPublishesArticleUpdateEvent() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(visibleArticle());
        when(articleTranslationMapper.delete(any())).thenReturn(1);

        boolean deleted = newService().deleteSpecificTranslation(ARTICLE_ID, "en");

        assertTrue(deleted);
        verify(cacheService).evictArticleRelatedCache(ARTICLE_ID);

        ArticleSavedEvent event = captureEvent();
        assertEquals(ARTICLE_ID, event.getArticleId());
        assertEquals("UPDATE", event.getOperationType());
        assertTrue(Boolean.TRUE.equals(event.getViewStatus()));
        assertEquals("Hello-World", event.getPreviousArticleSlug());
    }

    @Test
    void deleteSpecificTranslation_doesNothingWhenNoRowDeleted() {
        when(articleTranslationMapper.delete(any())).thenReturn(0);

        boolean deleted = newService().deleteSpecificTranslation(ARTICLE_ID, "en");

        assertFalse(deleted);
        verifyNoInteractions(cacheService, eventPublisher);
        verify(articleMapper, never()).selectById(eq(ARTICLE_ID));
    }
}
