package com.ld.poetry.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.ArticleTranslation;

import java.util.List;

/**
 * 文章回收站服务。
 *
 * <p>文章删除走逻辑删除进回收站（翻译随文章保留），30 天保留期内可恢复；
 * 彻底删除仅限管理员网页端，自动化 API 不暴露该能力。</p>
 */
public interface ArticleRecycleService {

    /**
     * 分页查询回收站文章：站长可见全部，其他用户仅见本人文章。
     * 内部按登录态过滤：userId 为 null 表示不限定归属——API 场景（validateApiKey 已等同站长）
     * 无登录上下文，isBoss()=false 且 getUserId()=null，因此返回全站回收站。
     */
    IPage<Article> listTrash(long current, long size, String searchKey);

    /**
     * 从回收站恢复文章（作者或 Boss）。slug 冲突时自动追加 -restored 后缀并在消息中提示。
     */
    PoetryResult<String> restore(Integer articleId);

    /**
     * 从回收站恢复文章（站长身份，供自动化 API 使用：API 请求不经过 LoginCheck，
     * 无法从 PoetryUtil 取到当前用户，经 validateApiKey 校验后即等同站长操作）。
     */
    PoetryResult<String> restoreAsBoss(Integer articleId);

    /**
     * 移入回收站（站长身份，供自动化 API 使用，语义同 {@link #restoreAsBoss}）。
     * 翻译与历史版本保留，彻底删除仅限管理员网页端。
     */
    PoetryResult<String> deleteAsBoss(Integer articleId);

    /**
     * 彻底删除：物理删除文章行 + 全部翻译 + 全部历史版本。调用方须为 Boss。
     */
    PoetryResult purge(Integer articleId);

    /**
     * 恢复文章到指定历史版本（作者或 Boss）。
     * 恢复前先把当前版快照为 RESTORE 型，使恢复操作本身可反复撤销。
     */
    PoetryResult<String> restoreVersion(Integer articleId, Long versionId);

    /**
     * 恢复文章到指定历史版本（站长身份，供自动化 API 使用，语义同 {@link #restoreVersion}）。
     * 操作人记录为 null（API 无登录态），版本列表显示"未知"；需要标识操作方时请用三参重载。
     */
    default PoetryResult<String> restoreVersionAsBoss(Integer articleId, Long versionId) {
        return restoreVersionAsBoss(articleId, versionId, null);
    }

    /**
     * 恢复文章到指定历史版本（站长身份 + 操作人标识，供自动化 API 使用）：
     * actorUsername 写入版本快照的操作人，避免无登录态时记为 null。
     */
    PoetryResult<String> restoreVersionAsBoss(Integer articleId, Long versionId, String actorUsername);

    /**
     * 物理清理回收站中超期的文章（含翻译与版本），供每日定时任务调用。
     *
     * @param retentionDays 保留天数
     * @return 清理的文章数
     */
    int purgeExpiredTrash(int retentionDays);

    /**
     * 当前登录用户是否有该文章的管理权限（作者或 Boss，兼容回收站中的文章）。
     */
    boolean canManageArticle(Integer articleId);

    /**
     * 读取回收站文章的完整内容（含正文），供站长/自动化 API 在恢复前判断恢复对象。
     * 不在回收站时返回 null。
     */
    Article getTrashedArticle(Integer articleId);

    /**
     * 读取文章的全部语言翻译（供回收站详情/版本详情展示）。
     */
    List<ArticleTranslation> listArticleTranslations(Integer articleId);
}
