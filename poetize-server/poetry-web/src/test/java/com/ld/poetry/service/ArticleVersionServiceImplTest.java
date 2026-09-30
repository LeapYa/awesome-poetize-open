package com.ld.poetry.service;

import com.ld.poetry.dao.ArticleTranslationMapper;
import com.ld.poetry.dao.ArticleVersionMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.ArticleTranslation;
import com.ld.poetry.entity.ArticleVersion;
import com.ld.poetry.service.impl.ArticleVersionServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleVersionServiceImplTest {

    @Mock
    private ArticleVersionMapper articleVersionMapper;

    @Mock
    private ArticleTranslationMapper articleTranslationMapper;

    @Mock
    private com.ld.poetry.dao.ArticleMapper articleMapper;

    private ArticleVersionServiceImpl service;

    /** captureSnapshot 会在行锁内按 id 重读快照源，这里用可变引用模拟"数据库中的当前行" */
    private Article currentArticle;

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper.select(SFunction, ...) 需要实体的 MP 元数据缓存才能解析列名
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ArticleVersion.class);
        service = spy(new ArticleVersionServiceImpl());
        ReflectionTestUtils.setField(service, "articleTranslationMapper", articleTranslationMapper);
        ReflectionTestUtils.setField(service, "articleMapper", articleMapper);
        ReflectionTestUtils.setField(service, "baseMapper", articleVersionMapper);
        doReturn(true).when(service).save(any(ArticleVersion.class));
        when(articleVersionMapper.selectMaxVersionNo(any())).thenReturn(0);
        when(articleVersionMapper.selectLatestContentHash(any())).thenReturn(null);
        when(articleVersionMapper.selectList(any())).thenReturn(List.of());
        when(articleMapper.selectById(any())).thenAnswer(invocation -> currentArticle);
    }

    /** 模拟一次"当前行即入参文章"的快照调用 */
    private ArticleVersion capture(Article article) {
        currentArticle = article;
        return service.captureSnapshot(article.getId(), ArticleVersion.TYPE_UPDATE, 1, "admin");
    }

    private Article sampleArticle() {
        Article article = new Article();
        article.setId(7);
        article.setUserId(1);
        article.setArticleTitle("标题");
        article.setArticleSlug("slug");
        article.setArticleContent("# 正文\n内容段落");
        article.setSummary("摘要");
        article.setViewStatus(true);
        article.setCommentStatus(true);
        article.setRecommendStatus(false);
        article.setSubmitToSearchEngine(true);
        article.setPayType(0);
        article.setFreePercent(30);
        article.setSortId(2);
        article.setLabelId(3);
        return article;
    }

    @Test
    void captureSnapshot_shouldWriteFullRowAndTranslations() {
        Article article = sampleArticle();
        ArticleTranslation translation = new ArticleTranslation();
        translation.setArticleId(7);
        translation.setLanguage("en");
        translation.setTitle("Title");
        translation.setContent("Body");
        translation.setSummary("Summary");
        when(articleTranslationMapper.selectList(any())).thenReturn(List.of(translation));

        ArticleVersion version = capture(article);

        assertNotNull(version);
        assertEquals(1, version.getVersionNo());
        assertEquals("标题", version.getArticleTitle());
        assertNotNull(version.getTranslationsJson());
        assertTrue(version.getTranslationsJson().contains("\"language\":\"en\""));
        assertNotNull(version.getContentHash());

        ArgumentCaptor<ArticleVersion> captor = ArgumentCaptor.forClass(ArticleVersion.class);
        verify(service).save(captor.capture());
        assertEquals(version.getContentHash(), captor.getValue().getContentHash());
    }

    @Test
    void captureSnapshot_shouldSkipWhenContentUnchanged() {
        Article article = sampleArticle();

        // 第一次：正常写入
        ArticleVersion first = capture(article);
        assertNotNull(first);

        // 第二次：最新版本哈希与当前内容一致 → 跳过
        when(articleVersionMapper.selectLatestContentHash(7)).thenReturn(first.getContentHash());
        when(articleVersionMapper.selectMaxVersionNo(7)).thenReturn(1);
        ArticleVersion second = capture(article);

        assertNull(second);
        verify(service, times(1)).save(any(ArticleVersion.class));
    }

    @Test
    void captureSnapshot_shouldPruneVersionsBeyondLimit() {
        Article article = sampleArticle();

        // 已有 21 个版本（按 versionNo 降序返回），最旧的 v1 应被裁剪
        List<ArticleVersion> existing = new java.util.ArrayList<>();
        for (int i = 21; i >= 1; i--) {
            ArticleVersion version = new ArticleVersion();
            version.setId((long) i);
            version.setArticleId(7);
            version.setVersionNo(i);
            existing.add(version);
        }
        when(articleVersionMapper.selectList(any())).thenReturn(existing);

        capture(article);

        verify(articleVersionMapper).delete(any());
    }

    @Test
    void captureSnapshot_shouldNotPruneWhenWithinLimit() {
        List<ArticleVersion> existing = new java.util.ArrayList<>();
        for (int i = 1; i <= ArticleVersionServiceImpl.MAX_VERSIONS_PER_ARTICLE; i++) {
            ArticleVersion version = new ArticleVersion();
            version.setId((long) i);
            version.setArticleId(7);
            version.setVersionNo(i);
            existing.add(version);
        }
        when(articleVersionMapper.selectList(any())).thenReturn(existing);

        capture(sampleArticle());

        verify(articleVersionMapper, never()).delete(any());
    }

    @Test
    void captureSnapshot_shouldNotDeduplicateMetadataOnlyChange() {
        Article article = sampleArticle();

        // 第一次：正常写入，其哈希成为"最新版本哈希"
        ArticleVersion first = capture(article);
        assertNotNull(first);
        when(articleVersionMapper.selectLatestContentHash(7)).thenReturn(first.getContentHash());
        when(articleVersionMapper.selectMaxVersionNo(7)).thenReturn(1);

        // 只改封面：正文与翻译都没变，但这依然是"一次需要可回滚的变更"
        Article afterCoverOnlyEdit = sampleArticle();
        afterCoverOnlyEdit.setArticleCover("/static/cover/new.png");
        ArticleVersion second = capture(afterCoverOnlyEdit);

        assertNotNull(second, "仅元数据（封面）变化也必须落快照，否则该次变更无法回滚");
        verify(service, times(2)).save(any(ArticleVersion.class));
    }

    @Test
    void captureSnapshot_shouldNotDeduplicateVisibilityChange() {
        Article article = sampleArticle();

        ArticleVersion first = capture(article);
        when(articleVersionMapper.selectLatestContentHash(7)).thenReturn(first.getContentHash());
        when(articleVersionMapper.selectMaxVersionNo(7)).thenReturn(1);

        // 只切可见性（隐藏/公开）同样必须留下变更前的可见状态
        Article afterVisibilityEdit = sampleArticle();
        afterVisibilityEdit.setViewStatus(false);
        assertNotNull(capture(afterVisibilityEdit));
    }

    @Test
    void captureSnapshot_shouldTreatEquivalentPayAmountAsUnchanged() {
        Article article = sampleArticle();
        article.setPayAmount(new java.math.BigDecimal("30.00"));

        ArticleVersion first = capture(article);
        when(articleVersionMapper.selectLatestContentHash(7)).thenReturn(first.getContentHash());
        when(articleVersionMapper.selectMaxVersionNo(7)).thenReturn(1);

        // 30.0 与 30.00 数值等价，不应因精度差异制造重复版本
        Article sameAmount = sampleArticle();
        sameAmount.setPayAmount(new java.math.BigDecimal("30.0"));
        assertNull(capture(sameAmount));
        verify(service, times(1)).save(any(ArticleVersion.class));
    }

    @Test
    void captureSnapshot_shouldSkipNullArticle() {
        assertNull(service.captureSnapshot(null, ArticleVersion.TYPE_UPDATE, 1, "admin"));
        verify(articleVersionMapper, never()).insert(any(ArticleVersion.class));
    }
}
