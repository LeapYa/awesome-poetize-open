package com.ld.poetry.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ld.poetry.aop.AuditLog;
import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.ArticleVersion;
import com.ld.poetry.service.ArticleRecycleService;
import com.ld.poetry.service.ArticleVersionService;
import com.ld.poetry.service.impl.ArticleRecycleServiceImpl;
import com.ld.poetry.service.impl.ArticleVersionServiceImpl;
import com.ld.poetry.service.impl.ResourceTrashServiceImpl;
import com.ld.poetry.utils.PoetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 回收站管理（网页端）
 * </p>
 *
 * <p>文章删除后进回收站保留 30 天，期间可恢复；彻底删除仅站长（@LoginCheck(0)）可操作，
 * 自动化 API（/api）不暴露彻底删除能力。</p>
 */
@RestController
@RequestMapping("/admin/recycle")
@Slf4j
public class AdminRecycleController {

    @Autowired
    private ArticleRecycleService articleRecycleService;

    @Autowired
    private ArticleVersionService articleVersionService;

    /**
     * 回收站与历史版本保留策略：供管理端展示倒计时与提示文案，避免前端硬编码保留天数
     */
    @GetMapping("/config")
    @LoginCheck(1)
    public PoetryResult<Map<String, Integer>> retentionConfig() {
        Map<String, Integer> config = new LinkedHashMap<>();
        config.put("articleRetentionDays", ArticleRecycleServiceImpl.TRASH_RETENTION_DAYS);
        config.put("resourceRetentionDays", ResourceTrashServiceImpl.TRASH_RETENTION_DAYS);
        config.put("backupRetentionDays", ResourceTrashServiceImpl.TRASH_RETENTION_DAYS);
        config.put("versionRetentionDays", ArticleVersionServiceImpl.VERSION_RETENTION_DAYS);
        config.put("maxVersionsPerArticle", ArticleVersionServiceImpl.MAX_VERSIONS_PER_ARTICLE);
        return PoetryResult.success(config);
    }

    /**
     * 回收站文章分页列表
     */
    @PostMapping("/article/list")
    @LoginCheck(1)
    public PoetryResult<IPage<Article>> listTrash(@RequestBody Map<String, Object> params) {
        long current = params.get("current") instanceof Number number ? number.longValue() : 1;
        long size = params.get("size") instanceof Number number ? number.longValue() : 10;
        String searchKey = params.get("searchKey") == null ? null : params.get("searchKey").toString();
        return PoetryResult.success(articleRecycleService.listTrash(current, size, searchKey));
    }

    /**
     * 从回收站恢复文章
     */
    @PostMapping("/article/restore")
    @LoginCheck(1)
    @AuditLog(action = "ARTICLE_RESTORE", targetType = "ARTICLE", targetIdParam = "id", summary = "从回收站恢复文章")
    public PoetryResult restoreArticle(@RequestParam("id") Integer id) {
        return articleRecycleService.restore(id);
    }

    /**
     * 彻底删除文章（物理删除文章行、翻译与历史版本），仅站长可操作
     */
    @PostMapping("/article/purge")
    @LoginCheck(0)
    @AuditLog(action = "ARTICLE_PURGE", targetType = "ARTICLE", targetIdParam = "id", summary = "彻底删除文章")
    public PoetryResult purgeArticle(@RequestParam("id") Integer id) {
        return articleRecycleService.purge(id);
    }

    /**
     * 文章历史版本列表（新版本在前）
     */
    @GetMapping("/article/versionList")
    @LoginCheck(1)
    public PoetryResult<List<ArticleVersion>> versionList(@RequestParam("articleId") Integer articleId) {
        if (!isArticleOwnerOrBoss(articleId)) {
            return PoetryResult.fail("没有权限查看此文章版本！");
        }
        return PoetryResult.success(articleVersionService.listByArticleId(articleId));
    }

    /**
     * 版本详情（含正文与翻译快照）
     */
    @GetMapping("/article/versionDetail")
    @LoginCheck(1)
    public PoetryResult<ArticleVersion> versionDetail(@RequestParam("id") Long id) {
        ArticleVersion version = articleVersionService.getVersion(id);
        if (version == null) {
            return PoetryResult.fail("版本不存在！");
        }
        if (!isArticleOwnerOrBoss(version.getArticleId())) {
            return PoetryResult.fail("没有权限查看此文章版本！");
        }
        return PoetryResult.success(version);
    }

    /**
     * 恢复文章到指定历史版本（恢复前自动快照当前版，可反复撤销）
     */
    @PostMapping("/article/restoreVersion")
    @LoginCheck(1)
    @AuditLog(action = "ARTICLE_VERSION_RESTORE", targetType = "ARTICLE", targetIdParam = "articleId",
            summary = "恢复文章历史版本")
    public PoetryResult restoreVersion(@RequestParam("articleId") Integer articleId,
            @RequestParam("versionId") Long versionId) {
        return articleRecycleService.restoreVersion(articleId, versionId);
    }

    private boolean isArticleOwnerOrBoss(Integer articleId) {
        // 权限判断委托给回收站服务（文章在回收站时由服务层走 selectTrashedById 校验）
        return PoetryUtil.isBoss() || articleRecycleService.canManageArticle(articleId);
    }
}
