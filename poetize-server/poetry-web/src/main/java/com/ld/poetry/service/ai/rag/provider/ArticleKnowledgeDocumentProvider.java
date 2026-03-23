package com.ld.poetry.service.ai.rag.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.LabelMapper;
import com.ld.poetry.dao.SortMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.Label;
import com.ld.poetry.entity.Sort;
import com.ld.poetry.service.ai.rag.KnowledgeDocumentProvider;
import com.ld.poetry.service.ai.rag.KnowledgeSourceType;
import com.ld.poetry.service.ai.rag.KnowledgeVisibilityScope;
import com.ld.poetry.service.ai.rag.RagTextUtils;
import com.ld.poetry.service.ai.rag.dto.KnowledgeSourceDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ArticleKnowledgeDocumentProvider implements KnowledgeDocumentProvider {

    private final ArticleMapper articleMapper;
    private final SortMapper sortMapper;
    private final LabelMapper labelMapper;

    public ArticleKnowledgeDocumentProvider(ArticleMapper articleMapper, SortMapper sortMapper, LabelMapper labelMapper) {
        this.articleMapper = articleMapper;
        this.sortMapper = sortMapper;
        this.labelMapper = labelMapper;
    }

    @Override
    public String sourceType() {
        return KnowledgeSourceType.ARTICLE.name();
    }

    @Override
    public List<KnowledgeSourceDocument> listAllDocuments() {
        return articleMapper.selectList(indexableWrapper()).stream()
                .map(this::toDocument)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Optional<KnowledgeSourceDocument> getDocument(String sourceId) {
        if (!StringUtils.hasText(sourceId)) {
            return Optional.empty();
        }
        Article article = articleMapper.selectById(Integer.valueOf(sourceId));
        if (article == null || !isIndexable(article)) {
            return Optional.empty();
        }
        return toDocument(article);
    }

    private LambdaQueryWrapper<Article> indexableWrapper() {
        return new LambdaQueryWrapper<Article>()
                .eq(Article::getDeleted, false)
                .eq(Article::getViewStatus, true);
    }

    private boolean isIndexable(Article article) {
        if (article == null || Boolean.TRUE.equals(article.getDeleted()) || !Boolean.TRUE.equals(article.getViewStatus())) {
            return false;
        }
        if (StringUtils.hasText(article.getPassword())) {
            return false;
        }
        BigDecimal payAmount = article.getPayAmount();
        Integer payType = article.getPayType();
        return payType == null || payType == 0 || (payAmount != null && BigDecimal.ZERO.compareTo(payAmount) >= 0);
    }

    private Optional<KnowledgeSourceDocument> toDocument(Article article) {
        if (!isIndexable(article)) {
            return Optional.empty();
        }

        Sort sort = article.getSortId() != null ? sortMapper.selectById(article.getSortId()) : null;
        Label label = article.getLabelId() != null ? labelMapper.selectById(article.getLabelId()) : null;
        String title = StringUtils.hasText(article.getArticleTitle()) ? article.getArticleTitle() : "未命名文章";
        StringBuilder content = new StringBuilder();
        content.append("标题：").append(title).append("\n");
        if (sort != null && StringUtils.hasText(sort.getSortName())) {
            content.append("分类：").append(sort.getSortName()).append("\n");
        }
        if (label != null && StringUtils.hasText(label.getLabelName())) {
            content.append("标签：").append(label.getLabelName()).append("\n");
        }
        if (StringUtils.hasText(article.getSummary())) {
            content.append("摘要：").append(article.getSummary()).append("\n");
        }
        content.append("正文：").append(RagTextUtils.normalize(article.getArticleContent()));

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("articleId", article.getId());
        metadata.put("title", title);
        metadata.put("sortName", sort != null ? sort.getSortName() : "");
        metadata.put("labelName", label != null ? label.getLabelName() : "");
        metadata.put("updatedAt", article.getUpdateTime() != null ? article.getUpdateTime().toString() : "");

        LocalDateTime updatedAt = article.getUpdateTime() != null ? article.getUpdateTime() : article.getCreateTime();
        return Optional.of(new KnowledgeSourceDocument(
                sourceType(),
                String.valueOf(article.getId()),
                title,
                content.toString(),
                KnowledgeVisibilityScope.PUBLIC_ARTICLE,
                updatedAt,
                metadata));
    }
}
