package com.ld.poetry.utils;

import org.springframework.util.StringUtils;

/**
 * 异常诊断工具
 * 为审计日志生成可读的失败描述，便于定位失败环节
 */
public final class ExceptionDiagnosticUtil {

    private ExceptionDiagnosticUtil() {
    }

    /**
     * 异常的可诊断描述：类名 + 消息
     */
    public static String describe(Throwable cause) {
        if (cause == null) {
            return null;
        }
        String message = cause.getMessage();
        return StringUtils.hasText(message)
                ? cause.getClass().getSimpleName() + ": " + message
                : cause.getClass().getSimpleName();
    }

    /**
     * 最底层根因描述（与顶层异常不同时返回），便于定位真实失败点
     */
    public static String describeRootCause(Throwable cause) {
        if (cause == null) {
            return null;
        }
        Throwable root = cause;
        int depth = 0;
        while (root.getCause() != null && root.getCause() != root && depth++ < 8) {
            root = root.getCause();
        }
        return root == cause ? null : describe(root);
    }
}