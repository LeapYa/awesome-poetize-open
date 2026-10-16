package com.ld.poetry.controller.dto;

import java.util.List;

/**
 * 资源批量删除 / 移入回收站的请求体，由 batchDelete、batchDelete/preview、batchMoveToTrash 三个接口共用。
 *
 * <p>三个开关字段刻意使用包装类型 {@link Boolean} 而非原始类型 {@code boolean}：
 * Jackson 3 默认开启 {@code FAIL_ON_NULL_FOR_PRIMITIVES}，当调用方省略这些字段时
 * （/resource/batchMoveToTrash 只需要 targets），原始类型会因 null 反序列化直接失败并抛
 * {@code MismatchedInputException: Cannot map null into type boolean}，前端只能看到笼统的"服务异常"。
 * 紧凑构造器统一把 null 归一化为 false，因此访问器返回值永不为 null，调用点可安全拆箱。</p>
 */
public record ResourceBatchDeleteRequest(
        List<Target> targets,
        Boolean forceReferenced,
        Boolean removeMissingRecords,
        Boolean removeUnsupportedRecords
) {

    public ResourceBatchDeleteRequest {
        forceReferenced = forceReferenced != null && forceReferenced;
        removeMissingRecords = removeMissingRecords != null && removeMissingRecords;
        removeUnsupportedRecords = removeUnsupportedRecords != null && removeUnsupportedRecords;
    }

    public record Target(Integer resourceId, String expectedPath) {
    }
}
