package com.ld.poetry.aop;

import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.service.SysAuditLogService;
import com.ld.poetry.utils.PoetryUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
@Order(10)
@Slf4j
public class AuditLogAspect {

    @Autowired
    private SysAuditLogService sysAuditLogService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        Object result = null;
        boolean success = true;
        String message = null;
        try {
            result = joinPoint.proceed();
            success = resolveSuccess(result);
            message = resolveMessage(result);
            return result;
        } catch (Throwable throwable) {
            success = false;
            message = throwable.getMessage();
            throw throwable;
        } finally {
            record(joinPoint, auditLog, success, message, result);
        }
    }

    private void record(ProceedingJoinPoint joinPoint, AuditLog auditLog, boolean success, String message, Object result) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            HttpServletRequest request = PoetryUtil.getRequest();
            boolean apiRequest = request != null
                    && request.getAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE) instanceof Boolean;
            if (request != null) {
                detail.put("method", request.getMethod());
            }
            // API 上游错误可能带密钥或配置值，不保存原始错误文本。
            if (!apiRequest && StringUtils.hasText(message)) {
                detail.put("message", message);
            }

            String targetId = resolveTargetId(joinPoint, auditLog.targetIdParam(), request);
            String summary = auditLog.summary();
            if (apiRequest) {
                if (result instanceof PoetryResult<?> response) {
                    detail.put("resultCode", response.getCode());
                    if (success && response.getData() instanceof Map<?, ?> data) {
                        if (data.get("articleId") instanceof Number articleId && "ARTICLE".equals(auditLog.targetType())) {
                            targetId = articleId.toString();
                        }
                        if (data.get("taskId") instanceof String taskId) {
                            detail.put("taskId", taskId);
                        }
                        if (data.get("changed") instanceof Boolean changed) {
                            detail.put("changed", changed);
                        }
                    }
                }
                if ("ARTICLE_SECTION_UPDATE".equals(auditLog.action())) {
                    boolean dryRun = Boolean.TRUE.equals(findValueInArgs(joinPoint.getArgs(), "dryRun"));
                    detail.put("dryRun", dryRun);
                    if (dryRun) {
                        summary = "API预览文章章节更新（未写入）";
                    }
                }
            }
            sysAuditLogService.recordOperation(
                    auditLog.type(),
                    auditLog.action(),
                    success,
                    auditLog.targetType(),
                    targetId,
                    summary,
                    detail);
        } catch (Exception e) {
            log.debug("记录操作审计日志失败: action={}, error={}", auditLog.action(), e.getMessage());
        }
    }

    private boolean resolveSuccess(Object result) {
        if (result instanceof PoetryResult<?> poetryResult) {
            return poetryResult.getCode() == 200;
        }
        return true;
    }

    private String resolveMessage(Object result) {
        if (result instanceof PoetryResult<?> poetryResult) {
            return poetryResult.getMessage();
        }
        return null;
    }

    private String resolveTargetId(ProceedingJoinPoint joinPoint, String targetIdParam, HttpServletRequest request) {
        if (!StringUtils.hasText(targetIdParam)) {
            return null;
        }
        if (request != null && request.getAttribute(CommonConst.API_AUTHENTICATED_ATTRIBUTE) instanceof Boolean) {
            Object value = findValueInArgs(joinPoint.getArgs(), targetIdParam);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        String targetId = null;
        if (request != null) {
            targetId = request.getParameter(targetIdParam);
            if (!StringUtils.hasText(targetId)) {
                @SuppressWarnings("unchecked")
                Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if (pathVariables != null) {
                    targetId = pathVariables.get(targetIdParam);
                }
            }
        }
        if (StringUtils.hasText(targetId)) {
            return targetId;
        }
        Object value = findValueInArgs(joinPoint.getArgs(), targetIdParam);
        return value == null ? null : String.valueOf(value);
    }

    private Object findValueInArgs(Object[] args, String key) {
        if (args == null || !StringUtils.hasText(key)) {
            return null;
        }
        for (Object arg : args) {
            Object value = findValue(arg, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object findValue(Object arg, String key) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof Map<?, ?> map) {
            return map.get(key);
        }
        String getterName = "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        try {
            Method method = arg.getClass().getMethod(getterName);
            return method.invoke(arg);
        } catch (Exception ignored) {
            return null;
        }
    }
}
