package com.ld.poetry.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.controller.dto.ResourceBatchDeleteRequest;
import com.ld.poetry.entity.Resource;
import com.ld.poetry.entity.ResourceContentReplacement;
import com.ld.poetry.entity.ResourceContentReplacementTarget;
import com.ld.poetry.entity.ResourceTrash;

import java.nio.file.Path;
import java.util.List;

/**
 * 资源回收站服务。
 *
 * <p>删除进回收站（不动物理文件，30 天保留期内可恢复）；替换成功后的旧文件备份
 * 保留登记，误替换可恢复。彻底删除/物理清理仅限管理员网页端与定时任务，
 * 自动化 API 不暴露该能力。</p>
 */
public interface ResourceTrashService {

    /**
     * 分页查询回收站中的资源。
     */
    IPage<Resource> listTrash(long current, long size, String searchKey);

    /**
     * 资源移入回收站（被引用且非 force 时拒绝）。
     */
    Resource moveToTrash(Integer resourceId, boolean forceReferenced);

    /**
     * 按访问路径移入回收站；路径不存在时抛出 IllegalArgumentException。
     */
    Resource moveToTrashByPath(String path, boolean forceReferenced);

    /**
     * 批量移入回收站。返回逐项结果（成功/失败原因）。
     */
    ResourceBatchMoveToTrashResult batchMoveToTrash(List<ResourceBatchDeleteRequest.Target> targets,
                                                    boolean forceReferenced);

    /**
     * 从回收站恢复资源。
     */
    Resource restoreTrash(Integer resourceId);

    /**
     * 彻底删除回收站资源（TRASH → 删除声明 → 物理删除管线）。
     */
    PoetryResult<?> purgeTrash(Integer resourceId);

    /**
     * 物理清理回收站中超期资源，供每日定时任务调用。
     *
     * @return 清理的资源数
     */
    int purgeExpiredTrash(int retentionDays);

    /**
     * 兜底回收：把"物理副本已全部处理完、却仍停留在 DELETION_PENDING"的资源退回回收站。
     * 用于删除流程被中断（进程崩溃、收尾校验失败）时恢复用户的可见与恢复入口，供每日任务调用。
     *
     * @return 退回回收站的资源数
     */
    int reclaimStalePendingToTrash();

    /**
     * 分页查询替换旧版备份（可按资源过滤）。
     */
    IPage<ResourceTrash> listBackups(long current, long size, Integer resourceId);

    /**
     * 按ID查询单个替换备份登记行（用于详情/预览）。
     */
    ResourceTrash getBackup(Long trashId);

    /**
     * 替换事务成功后登记旧文件备份（幂等：同一备份路径只登记一次）。
     * 备份文件缺失（如事务回滚恢复已用掉备份）时静默跳过。
     */
    void registerReplacementBackup(ResourceContentReplacement operation,
                                   ResourceContentReplacementTarget target,
                                   Path backupPath);

    /**
     * 恢复替换旧版：校验备份哈希 → 把当前文件对称备份进回收站 → 原子移回旧文件 →
     * 回写 resource 行的内容哈希/大小/MIME。
     */
    PoetryResult<String> restoreBackup(Long trashId);

    /**
     * 删除单个备份（物理删除备份文件 + 删除登记行）。
     */
    PoetryResult deleteBackup(Long trashId);

    /**
     * 物理清理超期备份，供每日定时任务调用。
     *
     * @return 清理的备份数
     */
    int purgeExpiredBackups(int retentionDays);

    /**
     * 预览回收站资源字节：供站长/自动化 API 在恢复前判断"该恢复哪一个"。
     *
     * <p>刻意绕过 content_state 门控（回收站资源本应不可读），因此调用方必须先做站长级鉴权。</p>
     */
    ResourcePreview previewTrashResource(Integer resourceId);

    /**
     * 预览替换备份文件字节（同样是站长级能力，用于判断恢复哪一份旧版）。
     */
    ResourcePreview previewBackup(Long trashId);

    /**
     * 预览载荷：原始字节 + 内容类型
     */
    record ResourcePreview(byte[] bytes, String contentType) {
    }

    /**
     * 批量移入回收站结果：moved 数 + 逐项信息。
     */
    record ResourceBatchMoveToTrashResult(int total, int moved, int failed,
                                          List<Item> items) {

        public record Item(Integer resourceId, String path, boolean success, String message) {
        }
    }
}
