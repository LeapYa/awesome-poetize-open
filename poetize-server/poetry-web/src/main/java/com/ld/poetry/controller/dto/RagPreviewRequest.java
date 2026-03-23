package com.ld.poetry.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record RagPreviewRequest(
        @NotBlank(message = "预览 query 不能为空") @Size(max = 2000, message = "预览 query 不能超过 2000 字符") String query,
        Map<String, Object> pageContext) {

    public RagPreviewRequest {
        if (pageContext == null) {
            pageContext = Map.of();
        }
    }
}
