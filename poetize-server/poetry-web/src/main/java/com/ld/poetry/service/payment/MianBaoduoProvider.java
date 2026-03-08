package com.ld.poetry.service.payment;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 面包多Pay Provider（骨架）
 * <p>
 * 面包多内容变现平台支付，适用于知识付费场景。
 * 使用 HMAC-SHA256 签名验证 Webhook 回调。
 * 需要在后台插件管理中配置 appId、appSecret 等参数后激活。
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-18
 */
@Slf4j
@Component
public class MianBaoduoProvider implements PaymentProvider {

    @Override
    public String getPlatformKey() {
        return "mianbaoduo";
    }

    @Override
    public String getPaymentUrl(Integer articleId, Integer userId, Map<String, Object> config) {
        String appId = (String) config.get("appId");
        String appSecret = (String) config.get("appSecret");

        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("面包多Pay尚未配置，请在后台插件管理中填写 appId 和 appSecret");
        }

        // TODO: 调用面包多 API 创建订单
        // 1. 构造请求体：app_id, out_trade_no, total_fee, body
        // 2. HMAC-SHA256 签名
        // 3. POST 请求创建订单
        // 4. 返回支付页面 URL
        String customOrderId = "u" + userId + "_a" + articleId;
        throw new UnsupportedOperationException("面包多Pay接入开发中，敬请期待");
    }

    @Override
    public CallbackResult verifyCallback(HttpServletRequest request, Map<String, Object> config) {
        // TODO: 验证面包多 Webhook 签名
        // 1. 读取请求体 JSON
        // 2. 使用 appSecret 对请求体做 HMAC-SHA256
        // 3. 与 X-MBD-Signature 头比对
        // 4. 解析订单信息：order_id, out_trade_no, amount, status
        log.warn("面包多Pay回调处理尚未实现");
        return null;
    }

    @Override
    public boolean testConnection(Map<String, Object> config) {
        String appId = (String) config.get("appId");
        if (appId == null || appId.isBlank()) {
            log.warn("面包多Pay未配置 appId");
            return false;
        }
        log.info("面包多Pay连接测试：该平台尚未完整接入，配置项已检测到 appId={}", appId);
        return false;
    }

    @Override
    public String getSuccessResponse() {
        return "{\"code\":0,\"msg\":\"success\"}";
    }
}
