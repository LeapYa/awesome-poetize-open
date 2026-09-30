package com.ld.poetry.controller;

import com.ld.poetry.aop.AuditLog;
import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.controller.dto.ResourceBatchDeleteRequest;
import com.ld.poetry.controller.dto.ResourceBatchDeleteResult;
import com.ld.poetry.service.ResourceBatchDeleteService;
import com.ld.poetry.service.ResourceTrashService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ConcurrentModificationException;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class ResourceBatchController {

    private final ResourceBatchDeleteService resourceBatchDeleteService;
    private final ResourceTrashService resourceTrashService;

    @PostMapping("/batchDelete/preview")
    @LoginCheck(0)
    public PoetryResult<ResourceBatchDeleteResult> previewDelete(
            @RequestBody ResourceBatchDeleteRequest request) {
        try {
            return PoetryResult.success(resourceBatchDeleteService.preview(request));
        } catch (ConcurrentModificationException e) {
            // 引用计数在预检期间发生变化（删除声明后新增业务引用），返回 409 让前端刷新
            return PoetryResult.fail(409, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return PoetryResult.fail(e.getMessage());
        }
    }

    @PostMapping("/batchDelete")
    @LoginCheck(0)
    @AuditLog(action = "RESOURCE_BATCH_DELETE", targetType = "RESOURCE", summary = "批量彻底删除资源")
    public PoetryResult<ResourceBatchDeleteResult> delete(
            @RequestBody ResourceBatchDeleteRequest request) {
        try {
            return PoetryResult.success(resourceBatchDeleteService.delete(request));
        } catch (ConcurrentModificationException e) {
            // 副本清理期间新增引用或活动副本被并发迁移/激活，返回 409 让前端刷新副本状态
            return PoetryResult.fail(409, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return PoetryResult.fail(e.getMessage());
        }
    }

    /**
     * 批量移入回收站（前端默认删除入口）：不动物理文件，保留期内可恢复
     */
    @PostMapping("/batchMoveToTrash")
    @LoginCheck(0)
    @AuditLog(action = "RESOURCE_BATCH_TRASH", targetType = "RESOURCE", summary = "批量移入回收站")
    public PoetryResult<ResourceTrashService.ResourceBatchMoveToTrashResult> batchMoveToTrash(
            @RequestBody ResourceBatchDeleteRequest request,
            @RequestParam(value = "forceReferenced", defaultValue = "false") boolean forceReferenced) {
        try {
            return PoetryResult.success(resourceTrashService.batchMoveToTrash(
                    request == null ? null : request.targets(), forceReferenced));
        } catch (IllegalArgumentException e) {
            return PoetryResult.fail(e.getMessage());
        }
    }
}