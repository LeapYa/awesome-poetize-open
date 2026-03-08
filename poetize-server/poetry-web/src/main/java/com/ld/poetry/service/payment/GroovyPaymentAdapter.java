package com.ld.poetry.service.payment;

import com.ld.poetry.plugin.GroovyPluginEngine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Groovy 支付插件桥接器
 * <p>
 * 作为 {@link PaymentProvider} 的特殊实现，将所有调用委托给对应的 Groovy 脚本。
 * 通过 {@link ThreadLocal} 持有当前被代理的 pluginKey，由 {@link PaymentService}
 * 在找到匹配的 Groovy 支付插件时调用 {@link #setCurrentPluginKey(String)} 设置。
 * </p>
 *
 * <p>
 * 与普通 Java Provider 的区别：
 * </p>
 * <ul>
 * <li>Java Provider：固定的 {@code platformKey}，编译时注册，功能完整</li>
 * <li>Groovy Provider：动态 {@code platformKey}，运行时加载，通过脚本实现</li>
 * </ul>
 *
 * @author LeapYa
 * @since 2026-02-19
 */
@Slf4j
@Component
public class GroovyPaymentAdapter implements PaymentProvider {

    @Autowired
    private GroovyPluginEngine groovyPluginEngine;

    /**
     * 当前线程正在代理的 Groovy 支付插件 key
     */
    private final ThreadLocal<String> currentPluginKey = new ThreadLocal<>();

    /**
     * 设置当前线程要代理的 pluginKey，由 PaymentService 在路由时调用
     */
    public void setCurrentPluginKey(String key) {
        currentPluginKey.set(key);
    }

    /**
     * 清除当前线程的 pluginKey，防止 ThreadLocal 泄漏
     */
    public void clearCurrentPluginKey() {
        currentPluginKey.remove();
    }

    @Override
    public String getPlatformKey() {
        return currentPluginKey.get();
    }

    // ============================================================
    // PaymentProvider 接口实现 —— 全部委托给 Groovy 脚本
    // ============================================================

    @Override
    public String getPaymentUrl(Integer articleId, Integer userId, Map<String, Object> config) {
        String key = requireKey();
        Map<String, Object> ctx = Map.of(
                "articleId", articleId,
                "userId", userId,
                "config", config);
        Object result = groovyPluginEngine.invokeMethod(key, "getPaymentUrl", ctx);
        if (result == null) {
            throw new IllegalStateException("Groovy 支付插件 [" + key + "] 的 getPaymentUrl 方法未返回支付链接");
        }
        return result.toString();
    }

    @Override
    public CallbackResult verifyCallback(HttpServletRequest request, Map<String, Object> config) {
        String key = requireKey();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("params", extractParams(request));
        ctx.put("config", config);

        Object result = groovyPluginEngine.invokeMethod(key, "verifyCallback", ctx);
        return convertToCallbackResult(result);
    }

    @Override
    public boolean testConnection(Map<String, Object> config) {
        String key = requireKey();
        Map<String, Object> ctx = Map.of("config", config);
        Object result = groovyPluginEngine.invokeMethod(key, "testConnection", ctx);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return result != null;
    }

    @Override
    public String getSuccessResponse() {
        String key = requireKey();
        Map<String, Object> ctx = Map.of();
        Object result = groovyPluginEngine.invokeMethod(key, "getSuccessResponse", ctx);
        return result != null ? result.toString() : "success";
    }

    // ============================================================
    // 内部工具方法
    // ============================================================

    private String requireKey() {
        String key = currentPluginKey.get();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "GroovyPaymentAdapter: 当前线程没有设置 pluginKey，请确保由 PaymentService 调用 setCurrentPluginKey()");
        }
        return key;
    }

    /**
     * 将 HttpServletRequest 的所有参数提取为 Map<String, String>
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return params;
    }

    /**
     * 将 Groovy 脚本返回的 Map 转换为 CallbackResult
     * <p>
     * Groovy 脚本的 verifyCallback 应返回包含以下 key 的 Map：
     * <ul>
     * <li>{@code verified} (Boolean) — 验签是否成功</li>
     * <li>{@code platformOrderId} (String) — 平台订单号</li>
     * <li>{@code platformUserId} (String) — 平台用户ID（可选）</li>
     * <li>{@code customOrderId} (String) — 自定义订单ID（格式: u{userId}_a{articleId}）</li>
     * <li>{@code amount} (BigDecimal/Number/String) — 支付金额</li>
     * <li>{@code parsedUserId} (Integer) — 已解析的博客用户ID（与 customOrderId 二选一）</li>
     * <li>{@code parsedArticleId} (Integer) — 已解析的文章ID（与 customOrderId 二选一）</li>
     * <li>{@code remark} (String) — 备注（可选）</li>
     * </ul>
     * </p>
     */
    @SuppressWarnings("unchecked")
    private CallbackResult convertToCallbackResult(Object groovyResult) {
        if (groovyResult == null) {
            log.warn("Groovy 支付插件 [{}] verifyCallback 返回了 null", currentPluginKey.get());
            return null;
        }

        if (!(groovyResult instanceof Map)) {
            log.warn("Groovy 支付插件 [{}] verifyCallback 返回了非 Map 类型: {}",
                    currentPluginKey.get(), groovyResult.getClass().getName());
            return null;
        }

        Map<String, Object> map = (Map<String, Object>) groovyResult;
        CallbackResult result = new CallbackResult();

        // verified
        Object verified = map.get("verified");
        result.setVerified(Boolean.TRUE.equals(verified));
        if (!result.isVerified()) {
            return result;
        }

        // 字段映射
        result.setPlatformOrderId(toStr(map.get("platformOrderId")));
        result.setPlatformUserId(toStr(map.get("platformUserId")));
        result.setCustomOrderId(toStr(map.get("customOrderId")));
        result.setRemark(toStr(map.get("remark")));

        // amount
        Object amount = map.get("amount");
        if (amount instanceof BigDecimal) {
            result.setAmount((BigDecimal) amount);
        } else if (amount instanceof Number) {
            result.setAmount(BigDecimal.valueOf(((Number) amount).doubleValue()));
        } else if (amount instanceof String) {
            try {
                result.setAmount(new BigDecimal((String) amount));
            } catch (NumberFormatException e) {
                log.warn("Groovy 插件 [{}] amount 格式错误: {}", currentPluginKey.get(), amount);
            }
        }

        // 支持两种方式传递用户/文章 ID：
        // 方式 1：脚本直接返回 parsedUserId + parsedArticleId
        Object parsedUserId = map.get("parsedUserId");
        Object parsedArticleId = map.get("parsedArticleId");
        if (parsedUserId instanceof Number && parsedArticleId instanceof Number) {
            result.setParsedUserId(((Number) parsedUserId).intValue());
            result.setParsedArticleId(((Number) parsedArticleId).intValue());
        } else if (result.getCustomOrderId() != null) {
            // 方式 2：通过 customOrderId 解析（格式: u{userId}_a{articleId}）
            result.parseCustomOrderId();
        }

        return result;
    }

    private String toStr(Object val) {
        return val != null ? val.toString() : null;
    }
}
