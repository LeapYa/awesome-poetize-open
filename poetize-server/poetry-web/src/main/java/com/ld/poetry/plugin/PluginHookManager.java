package com.ld.poetry.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

/**
 * 插件钩子管理器
 * <p>
 * 提供便捷方法在业务逻辑关键节点触发 Groovy 后端插件钩子。
 * 调用方只需注入此 Bean，无需关心 GroovyPluginEngine 的细节。
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-19
 */
@Slf4j
@Component
public class PluginHookManager {

    @Autowired
    private GroovyPluginEngine groovyPluginEngine;

    /**
     * 触发文章创建/保存后钩子
     * 
     * @param articleId 文章 ID
     * @param title     文章标题
     * @param userId    操作用户 ID
     */
    public void onArticleSave(Long articleId, String title, Long userId) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("articleId", articleId);
        ctx.put("title", title);
        ctx.put("userId", userId);
        triggerSafely("onArticleSave", ctx);
    }

    /**
     * 触发评论发布后钩子
     * 
     * @param commentId 评论 ID
     * @param articleId 所属文章 ID（null 表示留言板）
     * @param userId    评论用户 ID
     * @param content   评论内容（摘要，不超过 200 字符）
     */
    public void onCommentPublish(Long commentId, Long articleId, Long userId, String content) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("commentId", commentId);
        ctx.put("articleId", articleId);
        ctx.put("userId", userId);
        ctx.put("contentSnippet", content != null && content.length() > 200
                ? content.substring(0, 200) + "..."
                : content);
        triggerSafely("onCommentPublish", ctx);
    }

    /**
     * 触发用户注册后钩子
     * 
     * @param userId   用户 ID
     * @param username 用户名
     */
    public void onUserRegister(Long userId, String username) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("userId", userId);
        ctx.put("username", username);
        triggerSafely("onUserRegister", ctx);
    }

    /**
     * 触发自定义钩子（插件开发者可直接使用）
     * 
     * @param hookName 任意钩子名称
     * @param context  上下文
     */
    public void triggerHook(String hookName, Map<String, Object> context) {
        triggerSafely(hookName, context);
    }

    // ===== 内部方法 =====

    /**
     * 安全触发钩子，捕获异常不影响主流程
     */
    private void triggerSafely(String hookName, Map<String, Object> context) {
        try {
            groovyPluginEngine.triggerHook(hookName, context);
        } catch (Exception e) {
            log.error("触发插件钩子 [{}] 失败（已忽略，不影响主流程）", hookName, e);
        }
    }
}
