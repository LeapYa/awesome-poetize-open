package com.ld.poetry.controller.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归用例：/resource/batchMoveToTrash 的请求体只包含 targets 时，不得因缺失开关字段而反序列化失败。
 *
 * <p>事故背景（2026-10-16 线上）：Jackson 3 默认开启 {@code FAIL_ON_NULL_FOR_PRIMITIVES}，
 * 三个开关字段原为原始类型 {@code boolean}，调用方省略它们时抛
 * {@code MismatchedInputException: Cannot map `null` into type `boolean`}，
 * 导致"移入回收站"（含单条删除）全部不可用。修复方式是把字段改成包装类型并在紧凑构造器中归一化。</p>
 *
 * <p>本测试用于阻止任何人把这三个字段改回原始类型。</p>
 */
class ResourceBatchDeleteRequestTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("只传 targets：缺失的开关字段归一化为 false，不抛反序列化异常")
    void targetsOnlyIsAcceptedAndSwitchesDefaultToFalse() {
        String body = "{\"targets\":[{\"resourceId\":7,\"expectedPath\":\"/upload/a.png\"}]}";

        ResourceBatchDeleteRequest request = mapper.readValue(body, ResourceBatchDeleteRequest.class);

        assertThat(request.targets()).hasSize(1);
        assertThat(request.targets().get(0).resourceId()).isEqualTo(7);
        assertThat(request.targets().get(0).expectedPath()).isEqualTo("/upload/a.png");
        // 断言可直接当原始类型用（下游存在 !request.forceReferenced() 这类拆箱写法）
        assertThat(request.forceReferenced()).isFalse();
        assertThat(request.removeMissingRecords()).isFalse();
        assertThat(request.removeUnsupportedRecords()).isFalse();
    }

    @Test
    @DisplayName("完整请求体：三个开关按传入值生效（批量删除预检/执行的既有语义不变）")
    void fullBodyKeepsExplicitSwitchValues() {
        String body = "{\"targets\":[{\"resourceId\":1,\"expectedPath\":\"/a.png\"}],"
                + "\"forceReferenced\":true,\"removeMissingRecords\":true,\"removeUnsupportedRecords\":false}";

        ResourceBatchDeleteRequest request = mapper.readValue(body, ResourceBatchDeleteRequest.class);

        assertThat(request.forceReferenced()).isTrue();
        assertThat(request.removeMissingRecords()).isTrue();
        assertThat(request.removeUnsupportedRecords()).isFalse();
    }

    @Test
    @DisplayName("显式传 null：同样归一化为 false 而不是报错")
    void explicitNullIsTolerated() {
        String body = "{\"targets\":[],\"forceReferenced\":null,"
                + "\"removeMissingRecords\":null,\"removeUnsupportedRecords\":null}";

        ResourceBatchDeleteRequest request = mapper.readValue(body, ResourceBatchDeleteRequest.class);

        assertThat(request.targets()).isEmpty();
        assertThat(request.forceReferenced()).isFalse();
        assertThat(request.removeMissingRecords()).isFalse();
        assertThat(request.removeUnsupportedRecords()).isFalse();
    }
}
