package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.controller.dto.ResourceBatchDeleteRequest;
import com.ld.poetry.controller.dto.ResourceBatchDeleteResult;
import com.ld.poetry.dao.ResourceLocationMapper;
import com.ld.poetry.dao.ResourceMapper;
import com.ld.poetry.dao.ResourceTrashMapper;
import com.ld.poetry.entity.Resource;
import com.ld.poetry.entity.ResourceContentReplacement;
import com.ld.poetry.entity.ResourceContentReplacementTarget;
import com.ld.poetry.entity.ResourceLocation;
import com.ld.poetry.entity.ResourceTrash;
import com.ld.poetry.enums.ResourceContentState;
import com.ld.poetry.service.ResourceBatchDeleteService;
import com.ld.poetry.service.LocalResourceFileService;
import com.ld.poetry.service.ResourceDeletionStateService;
import com.ld.poetry.service.ResourceTrashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.ConcurrentModificationException;

/**
 * <p>
 * 资源回收站服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceTrashServiceImpl implements ResourceTrashService {

    /**
     * 回收站保留天数，超期由每日定时任务彻底清理
     */
    public static final int TRASH_RETENTION_DAYS = 30;

    /**
     * DELETION_PENDING 停留超过该分钟数且副本已全部终态，才由每日兜底任务退回回收站
     */
    public static final int RECLAIM_STALE_AFTER_MINUTES = 10;

    /**
     * 超期清理每批处理的条目数上限，避免一次性把全部过期 id 读进内存
     */
    private static final int PURGE_BATCH_SIZE = 200;

    /**
     * 在线预览单文件体积上限，避免大文件读出把内存打满
     */
    private static final long MAX_PREVIEW_BYTES = 20L * 1024 * 1024;

    /**
     * resource_trash.original_name 的列宽（resource 表同名字段为 512，登记时需按登记表宽度截断）
     */
    private static final int ORIGINAL_NAME_MAX_LENGTH = 256;

    private final ResourceDeletionStateService deletionStateService;
    private final ResourceBatchDeleteService resourceBatchDeleteService;
    private final ResourceMapper resourceMapper;
    private final ResourceTrashMapper resourceTrashMapper;
    private final ResourceLocationMapper resourceLocationMapper;
    private final LocalResourceFileService localResourceFileService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public IPage<Resource> listTrash(long current, long size, String searchKey) {
        Page<Resource> page = new Page<>(current > 0 ? current : 1, size > 0 ? Math.min(size, 100) : 10);
        return resourceMapper.selectTrashPage(page,
                StringUtils.hasText(searchKey) ? searchKey.trim() : null);
    }

    @Override
    public Resource moveToTrash(Integer resourceId, boolean forceReferenced) {
        Resource resource = deletionStateService.moveToTrash(resourceId, forceReferenced);
        log.info("资源已移入回收站: resourceId={}, path={}", resourceId, resource.getPath());
        return resource;
    }

    @Override
    public Resource moveToTrashByPath(String path, boolean forceReferenced) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("资源路径不能为空");
        }
        Resource found = resourceMapper.findByPath(path);
        if (found == null) {
            throw new IllegalArgumentException("文件不存在：" + path);
        }
        return moveToTrash(found.getId(), forceReferenced);
    }

    @Override
    public ResourceBatchMoveToTrashResult batchMoveToTrash(
            List<ResourceBatchDeleteRequest.Target> targets, boolean forceReferenced) {
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("请选择要删除的资源");
        }
        List<ResourceBatchMoveToTrashResult.Item> items = new java.util.ArrayList<>(targets.size());
        int moved = 0;
        int failed = 0;
        for (ResourceBatchDeleteRequest.Target target : targets) {
            try {
                Resource resource = deletionStateService.moveToTrash(target.resourceId(), forceReferenced);
                moved++;
                items.add(new ResourceBatchMoveToTrashResult.Item(
                        resource.getId(), resource.getPath(), true, "已移入回收站"));
            } catch (RuntimeException e) {
                failed++;
                items.add(new ResourceBatchMoveToTrashResult.Item(
                        target.resourceId(), target.expectedPath(), false,
                        StringUtils.hasText(e.getMessage()) ? e.getMessage() : "移入回收站失败"));
            }
        }
        return new ResourceBatchMoveToTrashResult(targets.size(), moved, failed, items);
    }

    @Override
    public Resource restoreTrash(Integer resourceId) {
        Resource resource = deletionStateService.restoreTrash(resourceId);
        log.info("资源已从回收站恢复: resourceId={}, path={}", resourceId, resource.getPath());
        return resource;
    }

    @Override
    public PoetryResult<?> purgeTrash(Integer resourceId) {
        Resource resource = deletionStateService.requireResource(resourceId);
        if (!ResourceContentState.TRASH.name().equals(resource.getContentState())) {
            return PoetryResult.fail("资源不在回收站中！");
        }
        // 先转入删除声明状态，再复用既有删除管线（含哈希校验与失败回滚）
        deletionStateService.transitionTrashToPending(resourceId);
        // forceReferenced=false：回收站期间若被新文章重新引用，退回回收站并提示，
        // 不静默删除仍被引用的内容（与直接删除的引用防线口径一致）
        ResourceBatchDeleteRequest request = new ResourceBatchDeleteRequest(
                List.of(new ResourceBatchDeleteRequest.Target(resource.getId(), resource.getPath())),
                false,
                true,
                false
        );
        ResourceBatchDeleteResult result;
        try {
            result = resourceBatchDeleteService.delete(request);
        } catch (Exception e) {
            // 管线在 claim 之前抛出（inspect/存储适配器异常）或进程中断时，资源会停在 DELETION_PENDING，
            // 而兜底 reclaim 要求副本全终态、超期清理只认 TRASH，两边都收不到它 → 永久卡死。
            // 这里显式退回回收站，避免"既不可恢复也不被清理"的中间态。
            log.error("彻底删除资源失败，正在退回回收站: resourceId={}", resourceId, e);
            rollbackPendingToTrashQuietly(resourceId);
            return PoetryResult.fail(500, "彻底删除失败；资源已退回回收站，可重试删除或恢复");
        }
        if (result.deletedCount() == 1) {
            // 资源本体已删除：连带清理其替换备份登记与备份文件，
            // 否则同名路径的新资源将来可能"恢复"到这些指向旧文件的备份
            purgeBackupsOfResource(resourceId);
            log.info("回收站资源已彻底删除: resourceId={}", resourceId);
            return PoetryResult.success(result);
        }
        // 删除未完成：退回回收站，避免资源卡在既不可恢复也不在回收站的 DELETION_PENDING 状态
        rollbackPendingToTrashQuietly(resourceId);
        String message = result.items().isEmpty()
                ? "彻底删除失败"
                : result.items().getFirst().message();
        return PoetryResult.fail(500, message + "；资源已退回回收站，可重试删除或恢复", result);
    }

    /**
     * 把处于 DELETION_PENDING 的资源退回回收站，失败只记日志（调用方仍需给用户明确结果）
     */
    private void rollbackPendingToTrashQuietly(Integer resourceId) {
        try {
            deletionStateService.rollbackPendingToTrash(resourceId);
        } catch (Exception e) {
            log.error("彻底删除失败后回退回收站状态失败: resourceId={}", resourceId, e);
        }
    }

    @Override
    public int purgeExpiredTrash(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays > 0 ? retentionDays
                : TRASH_RETENTION_DAYS);
        int purged = 0;
        // 分批处理，避免一次性把全部过期 id 读进内存；某批全部未产出进展时停止，防止死循环
        while (true) {
            List<Integer> expiredIds = resourceMapper.selectExpiredTrashIds(cutoff, PURGE_BATCH_SIZE);
            if (expiredIds == null || expiredIds.isEmpty()) {
                break;
            }
            int before = purged;
            for (Integer resourceId : expiredIds) {
                try {
                    PoetryResult<?> result = purgeTrash(resourceId);
                    if (result.getCode() == 200) {
                        purged++;
                    } else {
                        log.warn("回收站超期资源清理未完成: resourceId={}, message={}", resourceId, result.getMessage());
                    }
                } catch (Exception e) {
                    log.error("回收站超期资源清理失败: resourceId={}", resourceId, e);
                }
            }
            if (purged == before) {
                break;
            }
        }
        return purged;
    }

    @Override
    public int reclaimStalePendingToTrash() {
        // 删除管线（claim → 删文件 → finalize）正常分钟级完成；停留超过该时长才视为中断可回收，
        // 避免定时任务与恰好处于在途删除的资源互抢（把状态从 DELETION_PENDING 拉回 TRASH）
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(RECLAIM_STALE_AFTER_MINUTES);
        List<Integer> pendingIds = resourceMapper.selectUnfinalizedPendingIds(cutoff);
        int reclaimed = 0;
        for (Integer resourceId : pendingIds) {
            try {
                deletionStateService.rollbackPendingToTrash(resourceId);
                reclaimed++;
                log.warn("检测到删除未收尾的资源，已退回回收站: resourceId={}", resourceId);
            } catch (Exception e) {
                log.error("退回未收尾删除的资源失败: resourceId={}", resourceId, e);
            }
        }
        return reclaimed;
    }

    @Override
    public IPage<ResourceTrash> listBackups(long current, long size, Integer resourceId) {
        Page<ResourceTrash> page = new Page<>(current > 0 ? current : 1, size > 0 ? Math.min(size, 100) : 10);
        LambdaQueryWrapper<ResourceTrash> wrapper = new LambdaQueryWrapper<ResourceTrash>()
                .eq(resourceId != null, ResourceTrash::getResourceId, resourceId)
                .orderByDesc(ResourceTrash::getCreateTime);
        return resourceTrashMapper.selectPage(page, wrapper);
    }

    @Override
    public ResourceTrash getBackup(Long trashId) {
        return trashId == null ? null : resourceTrashMapper.selectById(trashId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerReplacementBackup(ResourceContentReplacement operation,
                                          ResourceContentReplacementTarget target,
                                          Path backupPath) {
        if (operation == null || target == null || backupPath == null
                || !Files.isRegularFile(backupPath)) {
            // 备份文件不存在（如 RESTORE_OLD 事务已用掉备份）→ 无需登记
            return;
        }
        Long exists = resourceTrashMapper.selectCount(new LambdaQueryWrapper<ResourceTrash>()
                .eq(ResourceTrash::getBackupPath, target.getBackupPath()));
        if (exists != null && exists > 0) {
            return;
        }
        Resource resource = resourceMapper.selectById(operation.getResourceId());
        ResourceTrash trash = new ResourceTrash();
        trash.setResourceId(operation.getResourceId());
        trash.setPublicId(resource == null ? null : resource.getPublicId());
        trash.setItemType(ResourceTrash.TYPE_REPLACEMENT_BACKUP);
        trash.setOriginalPath(target.getTargetPath());
        trash.setBackupPath(target.getBackupPath());
        trash.setBackupPathHash(backupPathHash(target.getBackupPath()));
        trash.setFileHash(StringUtils.hasText(target.getSourceHash())
                ? target.getSourceHash().toLowerCase(Locale.ROOT) : operation.getSourceHash());
        trash.setFileSize(safeFileSize(backupPath));
        trash.setMimeType(safeProbeContentType(backupPath));
        trash.setOriginalName(resource == null ? null : truncateOriginalName(resource.getOriginalName()));
        try {
            resourceTrashMapper.insert(trash);
            log.info("替换旧版备份已登记回收站: resourceId={}, backupPath={}",
                    operation.getResourceId(), target.getBackupPath());
        } catch (DataIntegrityViolationException e) {
            // 唯一键兜底：并发重复登记时忽略；warn 级避免静默丢失登记（孤儿备份文件不可见、不被清理）
            log.warn("替换旧版备份重复登记被忽略: backupPath={}", target.getBackupPath());
        }
    }

    @Override
    public PoetryResult<String> restoreBackup(Long trashId) {
        ResourceTrash trash = resourceTrashMapper.selectById(trashId);
        if (trash == null) {
            return PoetryResult.fail("备份不存在！");
        }
        Path backupPath = Paths.get(trash.getBackupPath());
        Path targetPath = Paths.get(trash.getOriginalPath());
        if (!Files.isRegularFile(backupPath)) {
            return PoetryResult.fail("备份文件已不存在，可能已被清理！");
        }

        // 归属校验：备份对应的资源必须仍存在，且原路径仍归它所有。
        // 否则（资源已彻底删除、同路径已被新资源占用）恢复会覆盖无关文件并改写错误的资源行。
        Resource owner = resourceMapper.selectById(trash.getResourceId());
        if (owner == null) {
            return PoetryResult.fail("备份对应的资源已不存在，无法恢复！");
        }
        if (!StringUtils.hasText(owner.getPath()) || !owner.getPath().equals(trash.getOriginalPath())) {
            return PoetryResult.fail("原路径已不再属于该资源（可能已被新资源占用），拒绝恢复备份！");
        }
        // 前置状态校验：替换在途（REPLACEMENT_PENDING）或彻底删除在途（DELETION_PENDING）时搬运文件，
        // 会与在途流程互相踩踏（哈希校验失败/文件被塞回），因此只允许对正常可读资源恢复旧版
        if (!Boolean.TRUE.equals(owner.getStatus())
                || !ResourceContentState.isActive(owner.getContentState())) {
            return PoetryResult.fail("资源当前状态不允许恢复备份（可能正在替换或删除中）！");
        }

        // 备份哈希校验：损坏的备份不允许回写
        if (StringUtils.hasText(trash.getFileHash())) {
            String actualHash;
            try (InputStream inputStream = Files.newInputStream(backupPath)) {
                actualHash = DigestUtils.sha256Hex(inputStream);
            } catch (Exception e) {
                return PoetryResult.fail("备份文件读取失败：" + e.getMessage());
            }
            if (!actualHash.equalsIgnoreCase(trash.getFileHash())) {
                return PoetryResult.fail("备份文件哈希校验失败，文件可能已损坏！");
            }
        }

        // 对称可撤销：把当前文件先备份进回收站
        if (Files.isRegularFile(targetPath)) {
            try {
                registerCurrentFileBackup(trash, targetPath);
            } catch (Exception e) {
                log.warn("恢复前备份当前文件失败，继续恢复旧版: targetPath={}", targetPath, e);
            }
        }

        // 先移动文件，再在事务中回写登记与资源摘要；文件已移动但 DB 失败时把文件搬回备份位置，
        // 保证"登记行 ↔ 备份文件"始终一致（注解式事务无法回滚文件系统操作）。
        if (!moveFile(backupPath, targetPath)) {
            return PoetryResult.fail("备份文件移回原路径失败：" + backupPath);
        }
        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.executeWithoutResult(status -> {
                LocalDateTime verifiedAt = LocalDateTime.now();
                boolean hasHash = StringUtils.hasText(trash.getFileHash());

                // 回写 resource 行的内容摘要（COMMIT_NEW 更新的逆操作）
                if (hasHash || trash.getFileSize() != null || StringUtils.hasText(trash.getMimeType())) {
                    LambdaUpdateWrapper<Resource> resourceUpdate = new LambdaUpdateWrapper<Resource>()
                            .eq(Resource::getId, trash.getResourceId());
                    if (hasHash) {
                        resourceUpdate.set(Resource::getResourceHash, trash.getFileHash())
                                .set(Resource::getHashSource, "TRASH_RESTORE")
                                .set(Resource::getHashVerifiedAt, verifiedAt);
                    }
                    if (trash.getFileSize() != null && trash.getFileSize() <= Integer.MAX_VALUE) {
                        resourceUpdate.set(Resource::getSize, trash.getFileSize().intValue());
                    }
                    if (StringUtils.hasText(trash.getMimeType())) {
                        resourceUpdate.set(Resource::getMimeType, trash.getMimeType());
                    }
                    if (resourceMapper.update(null, resourceUpdate) != 1) {
                        throw new ConcurrentModificationException("资源在恢复替换旧版期间发生变化");
                    }
                }

                // 同步活动物理副本摘要：/media 要求 resource.resource_hash 与 location.content_hash 严格相等
                // 且两者 verified_at 非空；只回写 resource 会让资源永久 503，且启动期哈希补齐不会选中它
                if (hasHash) {
                    Resource current = resourceMapper.selectByIdForUpdate(trash.getResourceId());
                    Long activeLocationId = current == null ? null : current.getActiveLocationId();
                    if (activeLocationId != null) {
                        ResourceLocation location = resourceLocationMapper.selectByIdForUpdate(activeLocationId);
                        if (location == null || !trash.getResourceId().equals(location.getResourceId())) {
                            throw new ConcurrentModificationException("活动物理副本不存在或归属不一致");
                        }
                        LambdaUpdateWrapper<ResourceLocation> locationUpdate = new LambdaUpdateWrapper<ResourceLocation>()
                                .eq(ResourceLocation::getId, location.getId())
                                .eq(ResourceLocation::getResourceId, trash.getResourceId())
                                .set(ResourceLocation::getContentHash, trash.getFileHash())
                                .set(ResourceLocation::getVerifiedAt, verifiedAt);
                        if (trash.getFileSize() != null) {
                            locationUpdate.set(ResourceLocation::getSize, trash.getFileSize());
                        }
                        if (StringUtils.hasText(trash.getMimeType())) {
                            locationUpdate.set(ResourceLocation::getMimeType, trash.getMimeType());
                        }
                        if (resourceLocationMapper.update(null, locationUpdate) != 1) {
                            throw new ConcurrentModificationException("活动物理副本在恢复替换旧版期间发生变化");
                        }
                    }
                }
                resourceTrashMapper.deleteById(trashId);
            });
        } catch (Exception e) {
            log.error("恢复替换旧版的数据库写入失败，正在把文件搬回备份位置: backup={}, target={}",
                    backupPath, targetPath, e);
            if (!moveFile(targetPath, backupPath)) {
                log.error("文件回滚失败，需人工处理: trashId={}, backup={}", trashId, backupPath);
            }
            return PoetryResult.fail("恢复替换旧版失败：" + e.getMessage());
        }
        log.info("替换旧版已恢复: resourceId={}, trashId={}", trash.getResourceId(), trashId);
        return PoetryResult.success("已恢复替换前的旧版本文件");
    }

    /**
     * 资源被彻底删除后清理其全部替换备份：先删物理备份文件，再删登记行。
     *
     * <p>备份文件删除失败只记日志（继续移除登记行），避免残留登记在后续同名路径上被误用。</p>
     */
    private int purgeBackupsOfResource(Integer resourceId) {
        List<ResourceTrash> backups = resourceTrashMapper.selectList(
                new LambdaQueryWrapper<ResourceTrash>().eq(ResourceTrash::getResourceId, resourceId));
        if (backups == null || backups.isEmpty()) {
            return 0;
        }
        for (ResourceTrash backup : backups) {
            try {
                Files.deleteIfExists(Paths.get(backup.getBackupPath()));
            } catch (Exception e) {
                log.warn("删除资源时清理备份文件失败，仅移除登记: backupPath={}", backup.getBackupPath(), e);
            }
        }
        int removed = resourceTrashMapper.deleteByResourceId(resourceId);
        log.info("已清理资源关联的替换备份: resourceId={}, 备份数={}", resourceId, removed);
        return removed;
    }

    /**
     * 删除备份：文件删除不可回滚，因此不做事务包裹，仅保证登记行在文件删除后移除
     */
    @Override
    public PoetryResult deleteBackup(Long trashId) {
        ResourceTrash trash = resourceTrashMapper.selectById(trashId);
        if (trash == null) {
            return PoetryResult.fail("备份不存在！");
        }
        try {
            Files.deleteIfExists(Paths.get(trash.getBackupPath()));
        } catch (Exception e) {
            log.warn("备份文件删除失败，仅移除登记: backupPath={}", trash.getBackupPath(), e);
        }
        resourceTrashMapper.deleteById(trashId);
        return PoetryResult.success();
    }

    /**
     * 移动文件：优先原子移动，失败时降级为普通移动；返回是否成功
     */
    private boolean moveFile(Path source, Path target) {
        try {
            Files.move(source, target,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (Exception atomicFailed) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Exception e) {
                log.error("文件移动失败: source={}, target={}", source, target, e);
                return false;
            }
        }
    }

    /**
     * 预览回收站资源字节：绕过 content_state 门控读回物理文件，仅限站长级调用方。
     * 只支持本地存储（远程对象需另行签发访问地址），且限制单次预览体积。
     */
    @Override
    public ResourcePreview previewTrashResource(Integer resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("资源ID不能为空");
        }
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null || !ResourceContentState.TRASH.name().equals(resource.getContentState())) {
            throw new IllegalStateException("资源不在回收站中");
        }
        String accessPath = null;
        String mimeType = resource.getMimeType();
        Long locationId = resource.getActiveLocationId();
        if (locationId != null) {
            ResourceLocation location = resourceLocationMapper.selectById(locationId);
            if (location != null) {
                accessPath = location.getAccessPath();
                if (StringUtils.hasText(location.getMimeType())) {
                    mimeType = location.getMimeType();
                }
            }
        }
        if (!StringUtils.hasText(accessPath)) {
            accessPath = resource.getPath();
        }
        if (!StringUtils.hasText(accessPath)) {
            throw new IllegalStateException("资源缺少可读取的物理副本地址");
        }
        Path path;
        try {
            path = localResourceFileService.resolveReadablePath(accessPath);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("预览文件不存在或不可读：" + e.getMessage(), e);
        }
        return readPreview(path, mimeType, resource.getOriginalName());
    }

    /**
     * 预览替换备份文件字节，仅限站长级调用方。
     */
    @Override
    public ResourcePreview previewBackup(Long trashId) {
        if (trashId == null) {
            throw new IllegalArgumentException("备份ID不能为空");
        }
        ResourceTrash trash = resourceTrashMapper.selectById(trashId);
        if (trash == null) {
            throw new IllegalArgumentException("备份不存在：" + trashId);
        }
        Path backupPath = Paths.get(trash.getBackupPath());
        if (!Files.isRegularFile(backupPath)) {
            throw new IllegalStateException("备份文件已不存在，可能已被清理");
        }
        return readPreview(backupPath, trash.getMimeType(), trash.getOriginalName());
    }

    /**
     * 读取预览字节并限制体积，避免大文件把内存打满
     */
    private ResourcePreview readPreview(Path path, String mimeType, String fallbackName) {
        try {
            long size = Files.size(path);
            if (size > MAX_PREVIEW_BYTES) {
                throw new IllegalStateException("文件过大，无法在线预览（超过 "
                        + (MAX_PREVIEW_BYTES / 1024 / 1024) + "MB）");
            }
            String contentType = StringUtils.hasText(mimeType)
                    ? mimeType
                    : inferPreviewContentType(path.getFileName() == null ? null : path.getFileName().toString(),
                    fallbackName);
            return new ResourcePreview(Files.readAllBytes(path), contentType);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("预览文件读取失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按物理文件名/原始文件名推断内容类型（物理副本扩展名优先）
     */
    private String inferPreviewContentType(String fileName, String fallbackName) {
        String extension = previewExtension(fileName);
        if (extension == null) {
            extension = previewExtension(fallbackName);
        }
        if (extension == null) {
            return "application/octet-stream";
        }
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "avif" -> "image/avif";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            case "pdf" -> "application/pdf";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mp3" -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }

    private String previewExtension(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String clean = name.replace('\\', '/');
        int queryIndex = clean.indexOf('?');
        if (queryIndex >= 0) {
            clean = clean.substring(0, queryIndex);
        }
        int dotIndex = clean.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == clean.length() - 1) {
            return null;
        }
        String extension = clean.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return extension.isEmpty() ? null : extension;
    }

    @Override
    public int purgeExpiredBackups(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays > 0 ? retentionDays
                : TRASH_RETENTION_DAYS);
        int purged = 0;
        while (true) {
            List<Long> expiredIds = resourceTrashMapper.selectExpiredIds(cutoff, PURGE_BATCH_SIZE);
            if (expiredIds == null || expiredIds.isEmpty()) {
                break;
            }
            int before = purged;
            for (Long trashId : expiredIds) {
                try {
                    if (deleteBackup(trashId).getCode() == 200) {
                        purged++;
                    }
                } catch (Exception e) {
                    log.error("超期备份清理失败: trashId={}", trashId, e);
                }
            }
            if (purged == before) {
                break;
            }
        }
        return purged;
    }

    /**
     * 恢复旧版前，把当前文件复制备份进回收站，使本次恢复可对称撤销
     */
    private void registerCurrentFileBackup(ResourceTrash trash, Path targetPath) throws Exception {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        Path preRestoreBackup = targetPath.resolveSibling(
                targetPath.getFileName() + "." + nonce + ".preRestore.backup");
        Files.copy(targetPath, preRestoreBackup);

        Resource resource = resourceMapper.selectById(trash.getResourceId());
        ResourceTrash currentBackup = new ResourceTrash();
        currentBackup.setResourceId(trash.getResourceId());
        currentBackup.setPublicId(resource == null ? null : resource.getPublicId());
        currentBackup.setItemType(ResourceTrash.TYPE_REPLACEMENT_BACKUP);
        currentBackup.setOriginalPath(trash.getOriginalPath());
        currentBackup.setBackupPath(preRestoreBackup.toString());
        currentBackup.setBackupPathHash(backupPathHash(preRestoreBackup.toString()));
        try (InputStream inputStream = Files.newInputStream(preRestoreBackup)) {
            currentBackup.setFileHash(DigestUtils.sha256Hex(inputStream));
        }
        currentBackup.setFileSize(Files.size(preRestoreBackup));
        currentBackup.setMimeType(safeProbeContentType(preRestoreBackup));
        currentBackup.setOriginalName(resource == null ? null : truncateOriginalName(resource.getOriginalName()));
        try {
            resourceTrashMapper.insert(currentBackup);
        } catch (DataIntegrityViolationException e) {
            Files.deleteIfExists(preRestoreBackup);
        }
    }

    /**
     * 备份路径的 SHA-256，作为 resource_trash 唯一键（规避 512 长路径前缀索引的冲突盲区）
     */
    private String backupPathHash(String backupPath) {
        return DigestUtils.sha256Hex(backupPath.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 按登记表列宽截断原始文件名，避免超长文件名插入失败或依赖数据库截断
     */
    private String truncateOriginalName(String originalName) {
        if (originalName == null || originalName.length() <= ORIGINAL_NAME_MAX_LENGTH) {
            return originalName;
        }
        return originalName.substring(0, ORIGINAL_NAME_MAX_LENGTH);
    }

    private Long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeProbeContentType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (Exception e) {
            return null;
        }
    }
}
