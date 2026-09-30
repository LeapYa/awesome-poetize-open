package com.ld.poetry.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ld.poetry.config.AsyncUserContext;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.ArticleTranslationMapper;
import com.ld.poetry.dao.ArticleVersionMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.ArticleVersion;
import com.ld.poetry.event.ArticleSavedEvent;
import com.ld.poetry.service.impl.ArticleRecycleServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleRecycleServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleVersionMapper articleVersionMapper;

    @Mock
    private ArticleTranslationMapper articleTranslationMapper;

    @Mock
    private com.ld.poetry.dao.ArticleDraftMapper articleDraftMapper;

    @Mock
    private com.ld.poetry.dao.ArticleDraftCollaboratorMapper articleDraftCollaboratorMapper;

    @Mock
    private com.ld.poetry.dao.ArticlePaymentMapper articlePaymentMapper;

    @Mock
    private com.ld.poetry.dao.CommentMapper commentMapper;

    @Mock
    private ArticleVersionService articleVersionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private ArticleRecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        // LambdaUpdateWrapper 需要实体的 MP 元数据缓存
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Article.class);

        service = new ArticleRecycleServiceImpl();
        ReflectionTestUtils.setField(service, "articleMapper", articleMapper);
        ReflectionTestUtils.setField(service, "articleVersionMapper", articleVersionMapper);
        ReflectionTestUtils.setField(service, "articleTranslationMapper", articleTranslationMapper);
        ReflectionTestUtils.setField(service, "articleDraftMapper", articleDraftMapper);
        ReflectionTestUtils.setField(service, "articleDraftCollaboratorMapper", articleDraftCollaboratorMapper);
        ReflectionTestUtils.setField(service, "articlePaymentMapper", articlePaymentMapper);
        ReflectionTestUtils.setField(service, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "articleVersionService", articleVersionService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(service, "cacheService", mock(CacheService.class));
        ReflectionTestUtils.setField(service, "transactionManager", transactionManager);
        when(transactionManager.getTransaction(any()))
                .thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());

        // 状态迁移 SQL 都带前置状态守卫并校验影响行数：成功路径需返回 1（模拟命中 1 行）
        when(articleMapper.softDeleteToTrash(any())).thenReturn(1);
        when(articleMapper.restoreFromTrash(any())).thenReturn(1);
        when(articleMapper.physicalDeleteById(any())).thenReturn(1);
        when(articleMapper.physicalDeleteExpiredTrash(any(), any())).thenReturn(1);

        // 权限校验依赖 PoetryUtil → AsyncUserContext；当前用户即文章作者
        AsyncUserContext.setUser(currentUser());
    }

    @AfterEach
    void tearDown() {
        AsyncUserContext.clear();
    }

    private com.ld.poetry.entity.User currentUser() {
        com.ld.poetry.entity.User user = new com.ld.poetry.entity.User();
        user.setId(1);
        user.setUsername("admin");
        return user;
    }

    private Article trashedArticle() {
        Article article = new Article();
        article.setId(7);
        article.setUserId(1);
        article.setArticleTitle("标题");
        article.setArticleSlug("slug");
        article.setArticleContent("正文");
        article.setViewStatus(true);
        article.setSortId(2);
        article.setLabelId(3);
        return article;
    }

    @Test
    void restore_shouldSuffixSlugWhenConflicting() {
        Article article = trashedArticle();
        when(articleMapper.selectTrashedById(7)).thenReturn(article);
        // 原 slug 与第一候选 -restored 都冲突，第二候选可用
        when(articleMapper.countBySlugIncludingTrashed(eq("slug"), eq(7))).thenReturn(1);
        when(articleMapper.countBySlugIncludingTrashed(eq("slug-restored"), eq(7))).thenReturn(1);
        when(articleMapper.countBySlugIncludingTrashed(eq("slug-restored-2"), eq(7))).thenReturn(0);

        PoetryResult<String> result = service.restore(7);

        assertTrue(result.isSuccess());
        verify(articleMapper).restoreFromTrash(7);
        ArgumentCaptor<ArticleSavedEvent> captor = ArgumentCaptor.forClass(ArticleSavedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        // previousArticleSlug 是"变更前别名"，用于清理旧别名的预渲染页，应为恢复前的 slug
        assertEquals("slug", captor.getValue().getPreviousArticleSlug());
        // 冲突时实际写回的行别名是解析后的可用值
        verify(articleMapper).update(any(), any());
        assertTrue(result.getMessage().contains("slug-restored-2"));
    }

    @Test
    void restore_shouldFailWhenArticleNotInTrash() {
        when(articleMapper.selectTrashedById(7)).thenReturn(null);
        assertEquals("文章不在回收站中！", service.restore(7).getMessage());
        verify(articleMapper, never()).restoreFromTrash(7);
    }

    @Test
    void restore_shouldReportOriginalSlugInEventSoPrerenderCleansOldPage() {
        Article article = trashedArticle();
        when(articleMapper.selectTrashedById(7)).thenReturn(article);
        // 删除前的别名仍被占用 → 恢复时会被改成 slug-restored
        when(articleMapper.countBySlugIncludingTrashed(eq("slug"), eq(7))).thenReturn(1);
        when(articleMapper.countBySlugIncludingTrashed(eq("slug-restored"), eq(7))).thenReturn(0);

        assertTrue(service.restore(7).isSuccess());

        ArgumentCaptor<ArticleSavedEvent> captor = ArgumentCaptor.forClass(ArticleSavedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        // 即使别名已改，事件也必须带删除前的原名，否则旧 URL 的静态页不会被清理
        assertEquals("slug", captor.getValue().getPreviousArticleSlug());
    }

    @Test
    void purge_shouldPhysicallyDeleteTranslationsVersionsAndArticle() {
        when(articleMapper.selectTrashedById(7)).thenReturn(trashedArticle());

        PoetryResult result = service.purge(7);

        assertTrue(result.isSuccess());
        verify(articleTranslationMapper).delete(any());
        verify(articleVersionMapper).deleteByArticleId(7);
        verify(articleMapper).physicalDeleteById(7);
        // 评论随文章一起物理清理，避免孤儿数据
        verify(commentMapper).delete(any());
        // 修订草稿、协作者与付费记录同样随文章物理清理；
        // 协作者按 article_id 清理依赖草稿行仍存在，必须先于草稿删除
        InOrder draftOrder = inOrder(articleDraftCollaboratorMapper, articleDraftMapper);
        draftOrder.verify(articleDraftCollaboratorMapper).physicalDeleteByArticleId(7);
        draftOrder.verify(articleDraftMapper).physicalDeleteByArticleId(7);
        verify(articlePaymentMapper).physicalDeleteByArticleId(7);
    }

    @Test
    void deleteAsBoss_shouldMoveArticleToTrashWithoutLoginContext() {
        Article article = trashedArticle();
        article.setId(11);
        when(articleMapper.selectById(11)).thenReturn(article);

        PoetryResult<String> result = service.deleteAsBoss(11);

        assertTrue(result.isSuccess());
        verify(articleMapper).softDeleteToTrash(11);
        verify(eventPublisher).publishEvent(any(ArticleSavedEvent.class));
    }

    @Test
    void deleteAsBoss_shouldFailWhenArticleMissingOrAlreadyTrashed() {
        when(articleMapper.selectById(12)).thenReturn(null);

        PoetryResult<String> result = service.deleteAsBoss(12);

        assertFalse(result.isSuccess());
        verify(articleMapper, never()).softDeleteToTrash(12);
    }

    @Test
    void restoreVersion_shouldSnapshotCurrentBeforeOverwriting() {
        Article article = trashedArticle();
        ArticleVersion version = new ArticleVersion();
        version.setId(99L);
        version.setArticleId(7);
        version.setVersionNo(3);
        version.setArticleTitle("旧标题");
        when(articleMapper.selectById(7)).thenReturn(article);
        when(articleVersionService.getVersion(99L)).thenReturn(version);

        PoetryResult<String> result = service.restoreVersion(7, 99L);

        assertTrue(result.isSuccess());
        InOrder inOrder = inOrder(articleVersionService, articleMapper);
        // 先快照当前版（RESTORE 型），再覆盖文章行
        inOrder.verify(articleVersionService).captureSnapshot(eq(7), eq(ArticleVersion.TYPE_RESTORE),
                any(), any());
        inOrder.verify(articleMapper).update(any(), any());
    }

    @Test
    void restoreVersion_shouldFailOnVersionMismatch() {
        Article article = trashedArticle();
        ArticleVersion version = new ArticleVersion();
        version.setId(99L);
        version.setArticleId(8);
        when(articleMapper.selectById(7)).thenReturn(article);
        when(articleVersionService.getVersion(99L)).thenReturn(version);

        assertEquals("版本不存在！", service.restoreVersion(7, 99L).getMessage());
        verify(articleVersionService, never()).captureSnapshot(any(), any(), any(), any());
    }

    @Test
    void restoreVersion_shouldSuffixSlugWhenVersionSlugAlreadyTaken() {
        Article article = trashedArticle();
        ArticleVersion version = new ArticleVersion();
        version.setId(99L);
        version.setArticleId(7);
        version.setVersionNo(3);
        version.setArticleSlug("taken-slug");
        when(articleMapper.selectById(7)).thenReturn(article);
        when(articleVersionService.getVersion(99L)).thenReturn(version);
        // 版本里的别名已被另一篇文章占用（唯一索引跨软删行生效），直接写回会撞键
        when(articleMapper.countBySlugIncludingTrashed(eq("taken-slug"), eq(7))).thenReturn(1);
        when(articleMapper.countBySlugIncludingTrashed(eq("taken-slug-restored"), eq(7))).thenReturn(0);

        PoetryResult<String> result = service.restoreVersion(7, 99L);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("taken-slug-restored"),
                "冲突时必须提示自动调整后的别名，实际: " + result.getMessage());
        verify(articleMapper).update(any(), any());
    }

    @Test
    void restoreVersion_shouldReportPreRestoreSlugInEventForPrerenderCleanup() {
        Article article = trashedArticle();
        article.setArticleSlug("old-slug");
        ArticleVersion version = new ArticleVersion();
        version.setId(99L);
        version.setArticleId(7);
        version.setVersionNo(3);
        version.setArticleSlug("new-slug");
        // 版本属于另一个分类/标签：事件第 4/5 参必须传恢复前的值，
        // 否则旧分类页的预渲染清理任务会丢失，静态页长期残留
        version.setSortId(20);
        version.setLabelId(30);
        when(articleMapper.selectById(7)).thenReturn(article);
        when(articleVersionService.getVersion(99L)).thenReturn(version);

        assertTrue(service.restoreVersion(7, 99L).isSuccess());

        ArgumentCaptor<ArticleSavedEvent> captor = ArgumentCaptor.forClass(ArticleSavedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        // 预渲染靠第 10 参清理旧别名的静态页；传成恢复后的新别名会让旧页面残留
        assertEquals("old-slug", captor.getValue().getPreviousArticleSlug());
        // 恢复后的分类/标签作为"新值"参数（第 2/3 参）
        assertEquals(20, captor.getValue().getSortId());
        assertEquals(30, captor.getValue().getLabelId());
        // 恢复前的分类/标签作为"变更前"参数（第 4/5 参），用于旧分类页清理
        assertEquals(2, captor.getValue().getPreviousSortId());
        assertEquals(3, captor.getValue().getPreviousLabelId());
    }
}
