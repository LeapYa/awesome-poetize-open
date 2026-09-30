package com.ld.poetry.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.SysAuditLog;
import com.ld.poetry.vo.SysAuditLogQueryVO;

import java.util.Map;

public interface SysAuditLogService extends IService<SysAuditLog> {

    PoetryResult<Page<SysAuditLog>> listLogs(SysAuditLogQueryVO queryVO);

    void recordLogin(String action, boolean success, String account, Integer userId, String username,
                     String summary, Map<String, Object> detail);

    void recordSecurity(String action, boolean success, String account, Integer userId, String username,
                        String summary, Map<String, Object> detail);

    /**
     * 记录安全事件（支持直传调用方 IP 与去重窗口），log_type='SECURITY'。
     *
     * <p>用于限流触发、IP 拦截、Token 失效等高频安全事件，防止审计表被攻击流量刷爆：
     * 同一 dedupKey 在 dedupWindowMillis 窗口内只落库第一条，其余抑制；
     * 上一窗口被抑制的次数随下一窗口首条记录写入 detail.suppressedInPrevWindow。
     *
     * @param callerIp          调用方 IP：非空时直接使用，为空时从当前 HTTP 请求上下文解析。
     *                          Filter 层等 RequestContextHolder 未就绪的场景必须显式传入
     * @param dedupKey          去重键（如 IP、账号、规则+限流键）；为空时不去重、直接落库
     * @param dedupWindowMillis 去重窗口毫秒数；非正数时取默认 60000
     */
    void recordSecurityDedup(String action, boolean success, String account, Integer userId, String username,
                             String summary, Map<String, Object> detail,
                             String callerIp, String dedupKey, long dedupWindowMillis);

    /**
     * 记录强制下线事件（action=FORCED_LOGOUT，log_type='SECURITY'）。
     *
     * @param userId   被下线的用户 ID
     * @param username 被下线的用户名（可空）
     * @param reason   下线原因（如：本人修改密码 / 忘记密码重置 / 管理员删除用户）
     */
    void recordForcedLogout(Integer userId, String username, String reason);

    void recordOperation(String logType, String action, boolean success, String targetType, String targetId,
                         String summary, Map<String, Object> detail);

    /**
     * 记录 AI 调用日志（log_type='AI'），包含 Token 用量。
     * 用于 AI 聊天/翻译/摘要等模型调用的可观测性追踪。
     *
     * <p>本重载依赖 {@code PoetryUtil} 解析调用主体，仅适用于 HTTP 请求线程内的同步调用。
     * 流式响应回调（reactor 线程）或异步任务（@Async 线程）请使用带 caller 参数的重载，
     * 由调用方在请求入口预先捕获 userId/username/ip/location 后传入。
     *
     * @param action            动作（AI_CHAT_STREAM / AI_CHAT / AI_COMMENT_REPLY / AI_TRANSLATE / AI_SUMMARY）
     * @param success           是否成功
     * @param targetType        目标对象类型（可空）
     * @param targetId         目标对象 ID（可空）
     * @param summary          摘要（建议放用户问题/文章标题预览）
     * @param detail           脱敏详情（mode/durationMs/messagePreview/responsePreview/principal 等）
     * @param promptTokens     输入 Token（可空，未上报时传 null）
     * @param completionTokens  输出 Token（可空）
     * @param totalTokens       合计 Token（可空）
     */
    void recordAi(String action, boolean success, String targetType, String targetId,
                  String summary, Map<String, Object> detail,
                  Integer promptTokens, Integer completionTokens, Integer totalTokens);

    /**
     * 记录 AI 调用日志（带调用主体信息）。
     *
     * <p>用于流式响应回调或异步任务线程：HTTP 请求上下文已丢失，
     * 调用方需在请求入口（HTTP 线程）预先捕获 caller 信息后传入。
     * 所有 caller 参数可空：传 null 时该字段留空（适用于无用户身份的后台任务）。
     *
     * @param callerUserId     调用者用户 ID（可空）
     * @param callerUsername    调用者用户名（可空）
     * @param callerIp         调用者 IP（可空）
     * @param callerLocation   调用者地理位置（可空，可由调用方解析或留空）
     */
    void recordAi(String action, boolean success, String targetType, String targetId,
                  String summary, Map<String, Object> detail,
                  Integer promptTokens, Integer completionTokens, Integer totalTokens,
                  Integer callerUserId, String callerUsername, String callerIp, String callerLocation);

    int cleanExpiredLogs(int retentionDays);
}
