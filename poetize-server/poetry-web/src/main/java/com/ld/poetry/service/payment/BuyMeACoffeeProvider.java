package com.ld.poetry.service.payment;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Buy Me a Coffee Provider（骨架）
 * <p>
 * 海外创作者赞助平台，通过 Webhook 接收赞助通知。
 * 使用 HMAC-SHA256 验证 Webhook 签名。
 * 需要在后台插件管理中配置 accessToken、webhookSecret 等参数后激活。
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-18
 */
@Slf4j
@Component
public class BuyMeACoffeeProvider implements PaymentProvider {

    @Override
    public String getPlatformKey() {
        return "buymeacoffee";
    }

    @Override
    public String getPaymentUrl(Integer articleId, Integer userId, Map<String, Object> config) {
        String accessToken = (String) config.get("accessToken");

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Buy Me a Coffee 尚未配置，请在后台插件管理中填写 accessToken");
        }

        // Buy Me a Coffee 不支持自定义订单，采用赞助页面 + Webhook 回调方式
        // 用户跳转到 BMC 页面赞助后，BMC 通过 Webhook 通知我们
        // TODO: 构造带 extra_info 的赞助链接
        // extra_info 中包含 customOrderId 用于关联用户和文章
        String customOrderId = "u" + userId + "_a" + articleId;
        throw new UnsupportedOperationException("Buy Me a Coffee 接入开发中，敬请期待");
    }

    @Override
    public CallbackResult verifyCallback(HttpServletRequest request, Map<String, Object> config) {
        // TODO: 验证 BMC Webhook 签名
        // 1. 读取请求体
        // 2. 使用 webhookSecret 做 HMAC-SHA256
        // 3. 与 X-BMC-Signature 头比对
        // 4. 解析赞助信息：supporter_name, support_id, amount, extra_info
        // 5. 从 extra_info 解析 customOrderId
        log.warn("Buy Me a Coffee 回调处理尚未实现");
        return null;
    }

    @Override
    public boolean testConnection(Map<String, Object> config) {
        String accessToken = (String) config.get("accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Buy Me a Coffee 未配置 accessToken");
            return false;
        }
        log.info("Buy Me a Coffee 连接测试：该平台尚未完整接入，配置项已检测到 accessToken");
        return false;
    }

    @Override
    public String getSuccessResponse() {
        return "{\"status\":\"ok\"}";
    }
}
