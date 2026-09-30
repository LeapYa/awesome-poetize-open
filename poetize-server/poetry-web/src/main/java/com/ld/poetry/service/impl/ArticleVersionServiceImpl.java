package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.ArticleTranslationMapper;
import com.ld.poetry.dao.ArticleVersionMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.ArticleTranslation;
import com.ld.poetry.entity.ArticleVersion;
import com.ld.poetry.service.ArticleVersionService;
import com.ld.poetry.service.SysAuditLogService;
import com.ld.poetry.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 文章历史版本快照服务实现类
 * </p>
 */
@Service
@Slf4j
public class ArticleVersionServiceImpl extends ServiceImpl<ArticleVersionMapper, ArticleVersion>
        implements ArticleVersionService {

    /**
     * 每篇文章保留的版本数上限，超出时从最旧开始裁剪
     */
    public static final int MAX_VERSIONS_PER_ARTICLE = 20;

    /**
     * 版本默认保留天数，超龄由每日定时任务统一清理
     */
    public static final int VERSION_RETENTION_DAYS = 90;

    /**
     * 超龄版本分批清理的每批上限，避免一次性大 IN 删除
     */
    private static final int PRUNE_BATCH_SIZE = 500;

    @Autowired
    private ArticleTranslationMapper articleTranslationMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SysAuditLogService sysAuditLogService;

    @Override
    public ArticleVersion captureSnapshot(Integer articleId, String snapshotType, Integer editorUserId,
                                          String editorUsername) {
        if (articleId == null) {
            return null;
        }
        // 事务内锁定文章行：并发更新同一文章时串行分配 version_no，避免重号
        articleMapper.lockRowById(articleId);
        // 锁只保证"发号"串行，不保证快照内容，因此必须在锁内重读整行作为快照源。
        // 若使用调用方事务外读到的实体，并发写同一文章会漏掉真实中间状态的快照
        // （表现为某次更新没有版本可回滚），或写出并非本次"更新前"真值的版本行（静默过度回滚）。
        Article snapshotSource = articleMapper.selectById(articleId);
        if (snapshotSource == null) {
            // 行已不存在或已进入回收站（@TableLogic 过滤），没有"更新前"状态可快照
            return null;
        }
        String translationsJson = buildTranslationsJson(snapshotSource.getId());
        // 哈希必须覆盖下方写入的全部快照字段：只改封面/可见性/付费等元数据时也要落快照，
        // 否则该次变更的"上一版"根本没有快照，恢复版本会跳回到更早的状态
        String contentHash = computeContentHash(snapshotSource, translationsJson);

        // 内容未变化则不重复快照（幂等），避免浏览量级操作制造垃圾版本
        String latestHash = baseMapper.selectLatestContentHash(snapshotSource.getId());
        if (contentHash.equals(latestHash)) {
            return null;
        }

        ArticleVersion version = new ArticleVersion();
        version.setArticleId(snapshotSource.getId());
        version.setVersionNo(baseMapper.selectMaxVersionNo(snapshotSource.getId()) + 1);
        version.setSnapshotType(snapshotType == null ? ArticleVersion.TYPE_UPDATE : snapshotType);
        version.setEditorUserId(editorUserId);
        version.setEditorUsername(editorUsername);
        version.setArticleTitle(snapshotSource.getArticleTitle());
        version.setArticleSlug(snapshotSource.getArticleSlug());
        version.setArticleContent(snapshotSource.getArticleContent());
        version.setSummary(snapshotSource.getSummary());
        version.setArticleCover(snapshotSource.getArticleCover());
        version.setVideoUrl(snapshotSource.getVideoUrl());
        version.setPassword(snapshotSource.getPassword());
        version.setTips(snapshotSource.getTips());
        version.setViewStatus(snapshotSource.getViewStatus());
        version.setCommentStatus(snapshotSource.getCommentStatus());
        version.setRecommendStatus(snapshotSource.getRecommendStatus());
        version.setSubmitToSearchEngine(snapshotSource.getSubmitToSearchEngine());
        version.setPayType(snapshotSource.getPayType());
        version.setPayAmount(snapshotSource.getPayAmount());
        version.setFreePercent(snapshotSource.getFreePercent());
        version.setSortId(snapshotSource.getSortId());
        version.setLabelId(snapshotSource.getLabelId());
        version.setPublishTime(snapshotSource.getPublishTime());
        version.setTranslationsJson(translationsJson);
        version.setContentHash(contentHash);
        version.setCreateTime(LocalDateTime.now());
        save(version);

        pruneVersions(snapshotSource.getId());
        log.info("已保存文章版本快照: 文章ID={}, 版本号={}, 类型={}, 操作人={}",
                snapshotSource.getId(), version.getVersionNo(), version.getSnapshotType(), editorUsername);
        return version;
    }

    @Override
    public List<ArticleVersion> listByArticleId(Integer articleId) {
        return lambdaQuery()
                .eq(ArticleVersion::getArticleId, articleId)
                .select(ArticleVersion::getId, ArticleVersion::getArticleId, ArticleVersion::getVersionNo,
                        ArticleVersion::getSnapshotType, ArticleVersion::getEditorUserId,
                        ArticleVersion::getEditorUsername, ArticleVersion::getArticleTitle,
                        ArticleVersion::getArticleSlug, ArticleVersion::getSummary,
                        ArticleVersion::getViewStatus, ArticleVersion::getCreateTime)
                .orderByDesc(ArticleVersion::getVersionNo)
                .list();
    }

    @Override
    public ArticleVersion getVersion(Long versionId) {
        return versionId == null ? null : getById(versionId);
    }

    @Override
    public int pruneExpiredVersions(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays > 0 ? retentionDays
                : VERSION_RETENTION_DAYS);
        // 分批删除：避免一次性大 IN 撞 max_allowed_packet/占位符上限；
        // 且查询只取"非该篇最新版"的行，保证每篇文章至少保留 1 个可回滚点
        int removed = 0;
        while (true) {
            List<Long> ids = baseMapper.selectPrunableExpiredIds(cutoff, PRUNE_BATCH_SIZE);
            if (ids == null || ids.isEmpty()) {
                break;
            }
            removeByIds(ids);
            removed += ids.size();
            if (ids.size() < PRUNE_BATCH_SIZE) {
                break;
            }
        }
        return removed;
    }

    /**
     * 按篇裁剪：超过 {@link #MAX_VERSIONS_PER_ARTICLE} 版时物理删除最旧的版本。
     * 只回读 id / version_no 两列（不回读 LONGTEXT 正文与翻译快照）。
     * 裁剪失败只记日志，不影响业务事务。
     */
    private void pruneVersions(Integer articleId) {
        try {
            List<ArticleVersion> versions = baseMapper.selectList(new LambdaQueryWrapper<ArticleVersion>()
                    .eq(ArticleVersion::getArticleId, articleId)
                    .select(ArticleVersion::getId, ArticleVersion::getVersionNo)
                    .orderByDesc(ArticleVersion::getVersionNo));
            if (versions.size() <= MAX_VERSIONS_PER_ARTICLE) {
                return;
            }
            List<Long> outdatedIds = versions.stream()
                    .skip(MAX_VERSIONS_PER_ARTICLE)
                    .map(ArticleVersion::getId)
                    .toList();
            baseMapper.delete(new LambdaQueryWrapper<ArticleVersion>()
                    .in(ArticleVersion::getId, outdatedIds));
            log.info("文章版本超出保留上限已裁剪: 文章ID={}, 裁剪数={}", articleId, outdatedIds.size());
        } catch (Exception e) {
            log.warn("文章版本裁剪失败: 文章ID={}, error={}", articleId, e.getMessage());
            recordPruneFailure(articleId, e);
        }
    }

    /**
     * 版本裁剪失败写入审计，避免只有滚动日志导致版本无限增长无法归因
     */
    private void recordPruneFailure(Integer articleId, Exception error) {
        if (sysAuditLogService == null) {
            return;
        }
        try {
            java.util.Map<String, Object> detail = new java.util.LinkedHashMap<>();
            detail.put("message", error == null ? null : error.getMessage());
            sysAuditLogService.recordOperation("OPERATION", "ARTICLE_VERSION_PRUNE_FAILED", false, "ARTICLE",
                    articleId == null ? null : String.valueOf(articleId), "文章版本裁剪失败", detail);
        } catch (Exception e) {
            log.warn("记录版本裁剪失败审计时出错: error={}", e.getMessage());
        }
    }

    /**
     * 将文章当前全部语言翻译序列化为快照 JSON：[{language,title,content,summary}]
     *
     * <p>确实没有翻译时返回 {@code null}；读取/序列化失败则抛异常，绝不以 {@code null} 降级。
     * 若失败也返回 null，快照会与"没有翻译"同形，之后恢复该版本会把文章全部翻译清空。</p>
     */
    private String buildTranslationsJson(Integer articleId) {
        List<ArticleTranslation> translations = articleTranslationMapper.selectList(
                new LambdaQueryWrapper<ArticleTranslation>()
                        .eq(ArticleTranslation::getArticleId, articleId));
        if (translations == null || translations.isEmpty()) {
            return null;
        }
        try {
            ArrayNode array = JsonUtils.createArrayNode();
            for (ArticleTranslation translation : translations) {
                ObjectNode node = array.addObject();
                node.put("language", translation.getLanguage());
                node.put("title", translation.getTitle());
                node.put("content", translation.getContent());
                node.put("summary", translation.getSummary());
            }
            String json = JsonUtils.toJsonString(array);
            if (json == null || json.isEmpty()) {
                throw new IllegalStateException("序列化结果为空");
            }
            return json;
        } catch (Exception e) {
            // 向上抛出：更新链路降级为"本次不产生快照"，恢复链路 fail-closed 取消本次恢复
            throw new IllegalStateException("文章翻译快照序列化失败: 文章ID=" + articleId, e);
        }
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(Character.forDigit((item >> 4) & 0xF, 16));
                builder.append(Character.forDigit(item & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            // 不允许写非法哈希（会污染 content_hash 并让去重静默失效）；
            // 由调用方把快照失败降级为告警，不影响文章更新本身
            throw new IllegalStateException("SHA-256 摘要算法不可用，无法生成版本快照", e);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 生成快照内容哈希，字段集合必须与 {@link #captureSnapshot} 写入的列保持一致。
     *
     * <p>只把标题/正文/翻译纳入哈希会导致"仅改封面、可见性、付费、分类标签"的更新被判定为
     * 无变化而跳过快照，使该次变更无法回滚，因此这里逐字段拼装。</p>
     */
    private String computeContentHash(Article article, String translationsJson) {
        return sha256Hex("title=" + nullToEmpty(article.getArticleTitle())
                + "\nslug=" + nullToEmpty(article.getArticleSlug())
                + "\ncontent=" + nullToEmpty(article.getArticleContent())
                + "\nsummary=" + nullToEmpty(article.getSummary())
                + "\ncover=" + nullToEmpty(article.getArticleCover())
                + "\nvideo=" + nullToEmpty(article.getVideoUrl())
                + "\npassword=" + nullToEmpty(article.getPassword())
                + "\ntips=" + nullToEmpty(article.getTips())
                + "\nviewStatus=" + article.getViewStatus()
                + "\ncommentStatus=" + article.getCommentStatus()
                + "\nrecommendStatus=" + article.getRecommendStatus()
                + "\nsubmitToSearchEngine=" + article.getSubmitToSearchEngine()
                + "\npayType=" + article.getPayType()
                // 30.0 与 30.00 语义相同，先归一化数值再入哈希，避免仅精度差异造出重复版本
                + "\npayAmount=" + (article.getPayAmount() == null
                        ? "" : article.getPayAmount().stripTrailingZeros().toPlainString())
                + "\nfreePercent=" + article.getFreePercent()
                + "\nsortId=" + article.getSortId()
                + "\nlabelId=" + article.getLabelId()
                + "\npublishTime=" + article.getPublishTime()
                + "\ntranslations=" + nullToEmpty(translationsJson));
    }
}
