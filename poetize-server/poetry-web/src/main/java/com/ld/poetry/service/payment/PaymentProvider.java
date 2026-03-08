package com.ld.poetry.service.payment;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 支付平台 Provider 接口
 * <p>
 * 策略模式：每个支付平台实现此接口，PaymentService 根据当前激活的插件路由到对应实现。
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-18
 */
public interface PaymentProvider {

    /**
     * 获取支付平台标识（对应 sys_plugin.plugin_key）
     */
    String getPlatformKey();

    /**
     * 生成支付链接或二维码URL
     *
     * @param articleId 文章ID
     * @param userId    博客用户ID
     * @param config    插件配置（JSON 解析后的 Map）
     * @return 支付链接URL
     */
    String getPaymentUrl(Integer articleId, Integer userId, Map<String, Object> config);

    /**
     * 验证回调签名并解析订单信息
     *
     * @param request HTTP 请求
     * @param config  插件配置
     * @return 回调结果，null 表示验签失败
     */
    CallbackResult verifyCallback(HttpServletRequest request, Map<String, Object> config);

    /**
     * 测试连接
     *
     * @param config 插件配置
     * @return 是否连接成功
     */
    boolean testConnection(Map<String, Object> config);

    /**
     * 获取回调成功时应返回的响应内容
     */
    String getSuccessResponse();
}
