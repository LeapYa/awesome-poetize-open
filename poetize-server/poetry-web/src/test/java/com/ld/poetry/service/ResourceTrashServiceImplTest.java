package com.ld.poetry.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.controller.dto.ResourceBatchDeleteRequest;
import com.ld.poetry.controller.dto.ResourceBatchDeleteResult;
import com.ld.poetry.dao.ResourceLocationMapper;
import com.ld.poetry.dao.ResourceMapper;
import com.ld.poetry.dao.ResourceTrashMapper;
import com.ld.poetry.entity.Resource;
import com.ld.poetry.entity.ResourceContentReplacement;
import com.ld.poetry.entity.ResourceContentReplacementTarget;
import com.ld.poetry.entity.ResourceTrash;
import com.ld.poetry.enums.ResourceContentState;
import com.ld.poetry.service.impl.ResourceTrashServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceTrashServiceImplTest {

    @Mock
    private ResourceDeletionStateService deletionStateService;

    @Mock
    private ResourceBatchDeleteService resourceBatchDeleteService;

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private ResourceTrashMapper resourceTrashMapper;

    @Mock
    private ResourceLocationMapper resourceLocationMapper;

    @Mock
    private LocalResourceFileService localResourceFileService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ResourceTrashServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // LambdaUpdateWrapper.set(SFunction, ...) 需要实体的 MP 元数据缓存才能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Resource.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                com.ld.poetry.entity.ResourceLocation.class);

        // TransactionTemplate 需要一个可用的 TransactionStatus；提交/回滚由 mock 兜底
        when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
        service = new ResourceTrashServiceImpl(
                deletionStateService, resourceBatchDeleteService, resourceMapper, resourceTrashMapper,
                resourceLocationMapper, localResourceFileService, transactionManager);
    }

    private Resource activeResource() {
        Resource resource = new Resource();
        resource.setId(5);
        resource.setPath("/static/img/a.png");
        resource.setStatus(true);
        resource.setContentState(ResourceContentState.ACTIVE.name());
        return resource;
    }

    @Test
    void moveToTrash_shouldDelegateToDeletionStateService() {
        when(deletionStateService.moveToTrash(5, false)).thenReturn(activeResource());

        Resource result = service.moveToTrash(5, false);

        assertEquals(5, result.getId());
        verify(deletionStateService).moveToTrash(5, false);
    }

    @Test
    void moveToTrashByPath_shouldFailWhenPathMissing() {
        when(resourceMapper.findByPath("/missing.png")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.moveToTrashByPath("/missing.png", false));
        verify(deletionStateService, never()).moveToTrash(any(), eq(false));
    }

    @Test
    void purgeTrash_shouldTransitionThenRunDeletePipeline() {
        Resource resource = activeResource();
        resource.setContentState(ResourceContentState.TRASH.name());
        resource.setStatus(false);
        when(deletionStateService.requireResource(5)).thenReturn(resource);
        when(resourceBatchDeleteService.delete(any())).thenReturn(new ResourceBatchDeleteResult(
                1, 1, 1, 0, 0, List.of()));

        PoetryResult<?> result = service.purgeTrash(5);

        assertTrue(result.isSuccess());
        // 先 TRASH → DELETION_PENDING，再走删除管线；forceReferenced=false 保留"新增引用"最后防线
        verify(deletionStateService).transitionTrashToPending(5);
        ArgumentCaptor<ResourceBatchDeleteRequest> captor =
                ArgumentCaptor.forClass(ResourceBatchDeleteRequest.class);
        verify(resourceBatchDeleteService).delete(captor.capture());
        assertFalse(captor.getValue().forceReferenced());
    }

    @Test
    void purgeTrash_shouldRejectWhenNotInTrash() {
        when(deletionStateService.requireResource(5)).thenReturn(activeResource());

        assertFalse(service.purgeTrash(5).isSuccess());
        verify(deletionStateService, never()).transitionTrashToPending(5);
        verify(resourceBatchDeleteService, never()).delete(any());
    }

    @Test
    void registerReplacementBackup_shouldSkipWhenBackupFileMissing() {
        service.registerReplacementBackup(new ResourceContentReplacement(),
                new ResourceContentReplacementTarget(), tempDir.resolve("not-exists.backup"));

        verify(resourceTrashMapper, never()).insert(any(ResourceTrash.class));
    }

    @Test
    void registerReplacementBackup_shouldSkipDuplicateBackupPath() {
        when(resourceTrashMapper.selectCount(any())).thenReturn(1L);

        ResourceContentReplacementTarget target = new ResourceContentReplacementTarget();
        target.setBackupPath("/static/img/a.png.123.backup");
        service.registerReplacementBackup(new ResourceContentReplacement(), target,
                tempDir.resolve("whatever"));

        verify(resourceTrashMapper, never()).insert(any(ResourceTrash.class));
    }

    @Test
    void registerReplacementBackup_shouldRecordTargetAndHash() throws Exception {
        Path backup = tempDir.resolve("a.png.123.backup");
        String content = "old-image-bytes";
        Files.writeString(backup, content);
        when(resourceTrashMapper.selectCount(any())).thenReturn(0L);

        Resource resource = activeResource();
        resource.setPublicId("pub-1");
        resource.setOriginalName("a.png");
        when(resourceMapper.selectById(5)).thenReturn(resource);

        ResourceContentReplacement operation = new ResourceContentReplacement();
        operation.setResourceId(5);
        operation.setSourceHash(DigestUtils.sha256Hex(content));

        ResourceContentReplacementTarget target = new ResourceContentReplacementTarget();
        target.setTargetPath("/static/img/a.png");
        target.setBackupPath(backup.toString());
        target.setSourceHash(DigestUtils.sha256Hex(content));

        service.registerReplacementBackup(operation, target, backup);

        ArgumentCaptor<ResourceTrash> captor = ArgumentCaptor.forClass(ResourceTrash.class);
        verify(resourceTrashMapper).insert(captor.capture());
        assertEquals(5, captor.getValue().getResourceId());
        assertEquals("/static/img/a.png", captor.getValue().getOriginalPath());
        assertEquals(DigestUtils.sha256Hex(content), captor.getValue().getFileHash());
        // M3: 唯一键走路径哈希列，长路径前缀不再可能撞键丢登记
        assertEquals(DigestUtils.sha256Hex(target.getBackupPath().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                captor.getValue().getBackupPathHash());
    }

    @Test
    void restoreBackup_shouldFailWhenBackupFileMissing() {
        ResourceTrash trash = new ResourceTrash();
        trash.setId(9L);
        trash.setResourceId(5);
        trash.setBackupPath(tempDir.resolve("missing.backup").toString());
        trash.setOriginalPath(tempDir.resolve("target.png").toString());
        when(resourceTrashMapper.selectById(9L)).thenReturn(trash);

        assertFalse(service.restoreBackup(9L).isSuccess());
        verify(resourceMapper, never()).update(any(), any());
    }

    @Test
    void restoreBackup_shouldFailOnHashMismatch() throws Exception {
        Path backup = tempDir.resolve("a.backup");
        Files.writeString(backup, "corrupted-content");
        ResourceTrash trash = new ResourceTrash();
        trash.setId(9L);
        trash.setResourceId(5);
        trash.setBackupPath(backup.toString());
        trash.setOriginalPath(tempDir.resolve("target.png").toString());
        trash.setFileHash(DigestUtils.sha256Hex("different"));
        when(resourceTrashMapper.selectById(9L)).thenReturn(trash);
        Resource owner = activeResource();
        owner.setPath(trash.getOriginalPath());
        when(resourceMapper.selectById(5)).thenReturn(owner);

        assertFalse(service.restoreBackup(9L).isSuccess());
        verify(resourceTrashMapper, never()).deleteById(9L);
    }

    @Test
    void restoreBackup_shouldFailWhenOwnerResourceMissing() throws Exception {
        Path backup = tempDir.resolve("missing-owner.backup");
        Files.writeString(backup, "old-bytes");
        ResourceTrash trash = new ResourceTrash();
        trash.setId(9L);
        trash.setResourceId(5);
        trash.setBackupPath(backup.toString());
        trash.setOriginalPath(tempDir.resolve("target.png").toString());
        trash.setFileHash(DigestUtils.sha256Hex("old-bytes"));
        when(resourceTrashMapper.selectById(9L)).thenReturn(trash);
        when(resourceMapper.selectById(5)).thenReturn(null);

        assertFalse(service.restoreBackup(9L).isSuccess());
        verify(resourceTrashMapper, never()).deleteById(9L);
    }

    @Test
    void restoreBackup_shouldFailWhenPathReassignedToAnotherResource() throws Exception {
        Path backup = tempDir.resolve("reassigned.backup");
        Files.writeString(backup, "old-bytes");
        ResourceTrash trash = new ResourceTrash();
        trash.setId(9L);
        trash.setResourceId(5);
        trash.setBackupPath(backup.toString());
        trash.setOriginalPath(tempDir.resolve("target.png").toString());
        trash.setFileHash(DigestUtils.sha256Hex("old-bytes"));
        when(resourceTrashMapper.selectById(9L)).thenReturn(trash);
        Resource owner = activeResource();
        owner.setPath(tempDir.resolve("other.png").toString());
        when(resourceMapper.selectById(5)).thenReturn(owner);

        assertFalse(service.restoreBackup(9L).isSuccess());
        verify(resourceTrashMapper, never()).deleteById(9L);
    }

    @Test
    void restoreBackup_shouldSyncResourceAndLocationSummaries() throws Exception {
        Path target = tempDir.resolve("a.png");
        Files.writeString(target, "new-bytes");
        Path backup = tempDir.resolve("a.backup");
        Files.writeString(backup, "old-bytes");

        ResourceTrash trash = new ResourceTrash();
        trash.setId(9L);
        trash.setResourceId(5);
        trash.setBackupPath(backup.toString());
        trash.setOriginalPath(target.toString());
        trash.setFileHash(DigestUtils.sha256Hex("old-bytes"));
        trash.setFileSize(Files.size(backup));
        trash.setMimeType("image/png");
        when(resourceTrashMapper.selectById(9L)).thenReturn(trash);

        Resource owner = activeResource();
        owner.setPath(target.toString());
        owner.setActiveLocationId(33L);
        when(resourceMapper.selectById(5)).thenReturn(owner);
        when(resourceMapper.selectByIdForUpdate(5)).thenReturn(owner);
        when(resourceMapper.update(any(), any())).thenReturn(1);

        com.ld.poetry.entity.ResourceLocation location = new com.ld.poetry.entity.ResourceLocation();
        location.setId(33L);
        location.setResourceId(5);
        when(resourceLocationMapper.selectByIdForUpdate(33L)).thenReturn(location);
        when(resourceLocationMapper.update(any(), any())).thenReturn(1);

        assertTrue(service.restoreBackup(9L).isSuccess());

        // B2：恢复旧版必须同时回写 resource 与 resource_location 摘要，
        // 否则 resource_hash 与 location.content_hash 不一致会导致 /media 永久 503
        verify(resourceMapper).update(any(), any());
        verify(resourceLocationMapper).update(any(), any());
        verify(resourceTrashMapper).deleteById(9L);
    }

    @Test
    void purgeTrash_shouldCleanReplacementBackupsAfterResourceDeleted() {
        Resource resource = activeResource();
        resource.setContentState(ResourceContentState.TRASH.name());
        resource.setStatus(false);
        ResourceTrash backup = new ResourceTrash();
        backup.setId(1L);
        backup.setResourceId(5);
        backup.setBackupPath(tempDir.resolve("b.backup").toString());
        when(deletionStateService.requireResource(5)).thenReturn(resource);
        when(resourceBatchDeleteService.delete(any())).thenReturn(new ResourceBatchDeleteResult(
                1, 1, 1, 0, 0, List.of()));
        when(resourceTrashMapper.selectList(any())).thenReturn(List.of(backup));

        PoetryResult<?> result = service.purgeTrash(5);

        assertTrue(result.isSuccess());
        // 资源本体删除后必须清理其替换备份登记，避免同名路径的新资源误恢复旧备份
        verify(resourceTrashMapper).deleteByResourceId(5);
    }

    @Test
    void purgeExpiredTrash_shouldPurgeEachExpiredResource() {
        when(resourceMapper.selectExpiredTrashIds(any(), anyInt()))
                .thenReturn(List.of(5, 6))
                .thenReturn(List.of());
        Resource trashed = activeResource();
        trashed.setContentState(ResourceContentState.TRASH.name());
        trashed.setStatus(false);
        when(deletionStateService.requireResource(any())).thenReturn(trashed);
        when(resourceBatchDeleteService.delete(any())).thenReturn(new ResourceBatchDeleteResult(
                1, 1, 1, 0, 0, List.of()));

        int purged = service.purgeExpiredTrash(30);

        assertEquals(2, purged);
        verify(deletionStateService).transitionTrashToPending(5);
        verify(deletionStateService).transitionTrashToPending(6);
    }

    @Test
    void purgeExpiredBackups_shouldDeleteEachExpiredBackup() {
        when(resourceTrashMapper.selectExpiredIds(any(), anyInt()))
                .thenReturn(List.of(1L, 2L))
                .thenReturn(List.of());
        ResourceTrash trash = new ResourceTrash();
        trash.setId(1L);
        when(resourceTrashMapper.selectById(1L)).thenReturn(trash);
        when(resourceTrashMapper.selectById(2L)).thenReturn(trash);

        int purged = service.purgeExpiredBackups(30);

        assertEquals(2, purged);
        verify(resourceTrashMapper).deleteById(1L);
        verify(resourceTrashMapper).deleteById(2L);
    }
}
