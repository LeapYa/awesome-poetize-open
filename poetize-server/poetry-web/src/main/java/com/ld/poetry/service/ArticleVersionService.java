package com.ld.poetry.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ld.poetry.entity.ArticleVersion;

import java.util.List;

/**
 * 文章历史版本快照服务。
 *
 * <p>每次更新文章前自动写入旧文全量快照（含全部语言翻译），
 * 恢复指定版本前也会先把当前版快照为 RESTORE 型，保证误操作可回溯。</p>
 */
public interface ArticleVersionService extends IService<ArticleVersion> {

    /**
     * 为文章当前状态落一份全量快照（在同一事务内调用，保证快照与后续变更一致）。
     *
     * <p>快照内容在行锁内按 {@code articleId} 重读，因此入参只需文章ID：
     * 调用方事务外读到的实体可能已过期，不能直接充当"更新前"状态。</p>
     *
     * <p>内容哈希与该文最新版本一致时跳过（幂等），避免无意义的重复版本；
     * 写入后按"每篇保留最近 {@code MAX_VERSIONS_PER_ARTICLE} 版"裁剪。</p>
     *
     * @param articleId      文章ID
     * @param snapshotType   快照类型 {@link ArticleVersion#TYPE_UPDATE} / {@link ArticleVersion#TYPE_RESTORE}
     * @param editorUserId   操作人用户ID（可空）
     * @param editorUsername 操作人用户名（可空）
     * @return 本次写入的版本；内容未变化被跳过、或文章行已不存在时返回 null
     */
    ArticleVersion captureSnapshot(Integer articleId, String snapshotType, Integer editorUserId, String editorUsername);

    /**
     * 按文章查询版本列表（新版本在前，仅摘要字段，不含正文大字段）。
     */
    List<ArticleVersion> listByArticleId(Integer articleId);

    /**
     * 查单个版本完整内容。
     */
    ArticleVersion getVersion(Long versionId);

    /**
     * 全局裁剪超龄版本（默认 90 天），供每日定时任务调用。
     *
     * @return 清理的版本数
     */
    int pruneExpiredVersions(int retentionDays);
}
