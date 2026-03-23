package com.ld.poetry.service.ai.rag.dto;

public record AdminNavigationAction(
        String type,
        String label,
        String route,
        String focusId,
        String panelKey,
        String sourceId) {

    public static final String TYPE_ADMIN_NAVIGATION = "admin_navigation";

    public AdminNavigationAction(String label, String route, String focusId, String panelKey, String sourceId) {
        this(TYPE_ADMIN_NAVIGATION, label, route, focusId, panelKey, sourceId);
    }
}
