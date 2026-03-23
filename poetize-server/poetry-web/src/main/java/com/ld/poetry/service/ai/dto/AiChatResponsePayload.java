package com.ld.poetry.service.ai.dto;

import com.ld.poetry.service.ai.rag.dto.AdminNavigationAction;

import java.util.List;

public record AiChatResponsePayload(
        String content,
        List<AdminNavigationAction> actions) {

    public static AiChatResponsePayload of(String content, List<AdminNavigationAction> actions) {
        return new AiChatResponsePayload(content != null ? content : "", actions != null ? actions : List.of());
    }
}
