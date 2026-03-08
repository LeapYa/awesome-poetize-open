package com.ld.poetry.service.payment;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;

/**
 * 易支付 V2 Provider
 * <p>
 * 第三方聚合支付平台，支持支付宝/微信/QQ钱包等多种支付方式。
 * V2 接口使用 SHA256WithRSA 签名。
 * </p>
 * <p>
 * 签名流程：
 * 1. 参数按 key ASCII 升序排序，用 & 连接成 key=value 字符串
 * 2. 使用商户 RSA 私钥做 SHA256WithRSA 签名
 * 3. 签名 Base64 编码后作为 sign 参数，sign_type=RSA
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-19
 */
@Slf4j
@Component
public class EpayProvider implements PaymentProvider {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getPlatformKey() {
        return "epay";
    }

    @Override
    public String getPaymentUrl(Integer articleId, Integer userId, Map<String, Object> config) {
        String apiUrl = getRequiredConfig(config, "apiUrl", "API地址");
        String pid = getRequiredConfig(config, "pid", "商户ID");
        String privateKeyStr = getRequiredConfig(config, "privateKey", "RSA私钥");
        String notifyUrl = (String) config.getOrDefault("notifyUrl", "");
        String returnUrl = (String) config.getOrDefault("returnUrl", "");

        // 获取支付金额
        BigDecimal amount = BigDecimal.valueOf(5.00);
        if (config.containsKey("fixedAmount")) {
            Object val = config.get("fixedAmount");
            if (val instanceof Number) {
                amount = BigDecimal.valueOf(((Number) val).doubleValue());
            }
        }

        String customOrderId = "u" + userId + "_a" + articleId + "_" + System.currentTimeMillis();

        // 构造参数（不含 sign 和 sign_type）
        Map<String, String> params = new TreeMap<>();
        params.put("pid", pid);
        params.put("type", "alipay"); // 默认支付宝，前端可扩展选择
        params.put("out_trade_no", customOrderId);
        params.put("notify_url", notifyUrl);
        params.put("return_url", returnUrl);
        params.put("name", articleId == 0 ? "会员订阅" : "文章解锁#" + articleId);
        params.put("money", amount.setScale(2).toPlainString());

        // SHA256WithRSA 签名
        String signStr = buildSignString(params);
        String sign;
        try {
            sign = rsaSign(signStr, privateKeyStr);
        } catch (Exception e) {
            log.error("易支付签名失败", e);
            throw new IllegalStateException("支付签名失败: " + e.getMessage());
        }

        // 构造支付 URL
        StringBuilder url = new StringBuilder(apiUrl);
        if (!apiUrl.endsWith("/"))
            url.append("/");
        url.append("submit.php?");
        params.forEach((k, v) -> url.append(urlEncode(k)).append("=").append(urlEncode(v)).append("&"));
        url.append("sign=").append(urlEncode(sign));
        url.append("&sign_type=RSA");

        return url.toString();
    }

    @Override
    public CallbackResult verifyCallback(HttpServletRequest request, Map<String, Object> config) {
        try {
            String epayPublicKey = (String) config.get("epayPublicKey");
            if (epayPublicKey == null || epayPublicKey.isBlank()) {
                log.warn("易支付平台公钥未配置");
                return null;
            }

            // 获取回调参数
            String tradeNo = request.getParameter("trade_no");
            String outTradeNo = request.getParameter("out_trade_no");
            String type = request.getParameter("type");
            String name = request.getParameter("name");
            String money = request.getParameter("money");
            String tradeStatus = request.getParameter("trade_status");
            String sign = request.getParameter("sign");

            if (tradeNo == null || outTradeNo == null || sign == null) {
                log.warn("易支付回调缺少必要参数");
                return null;
            }

            // 构造验签参数（去除 sign 和 sign_type）
            Map<String, String> params = new TreeMap<>();
            if (tradeNo != null)
                params.put("trade_no", tradeNo);
            if (outTradeNo != null)
                params.put("out_trade_no", outTradeNo);
            if (type != null)
                params.put("type", type);
            if (name != null)
                params.put("name", name);
            if (money != null)
                params.put("money", money);
            if (tradeStatus != null)
                params.put("trade_status", tradeStatus);

            // 添加其他可能的参数（不同易支付站点可能返回额外参数）
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                if (!"sign".equals(paramName) && !"sign_type".equals(paramName)
                        && !params.containsKey(paramName)) {
                    params.put(paramName, request.getParameter(paramName));
                }
            }

            // RSA-SHA256 验签
            String signStr = buildSignString(params);
            if (!rsaVerify(signStr, sign, epayPublicKey)) {
                log.warn("易支付回调验签失败, signStr={}", signStr);
                return null;
            }
            log.info("易支付回调验签成功, trade_no={}, out_trade_no={}", tradeNo, outTradeNo);

            // 检查交易状态
            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                log.info("易支付交易未完成, trade_status={}", tradeStatus);
                return null;
            }

            // 构建结果
            CallbackResult result = new CallbackResult();
            result.setVerified(true);
            result.setPlatformOrderId(tradeNo);
            result.setCustomOrderId(outTradeNo);
            result.setAmount(new BigDecimal(money));
            result.setStatus(1); // 已支付
            result.setRemark("type=" + type);

            // 解析 customOrderId（格式: u{userId}_a{articleId}_{timestamp}）
            parseEpayOrderId(result, outTradeNo);

            return result;

        } catch (Exception e) {
            log.error("易支付回调处理异常", e);
            return null;
        }
    }

    @Override
    public boolean testConnection(Map<String, Object> config) {
        String apiUrl = (String) config.get("apiUrl");
        String pid = (String) config.get("pid");
        String privateKey = (String) config.get("privateKey");

        if (apiUrl == null || apiUrl.isBlank()) {
            log.warn("易支付 API 地址未配置");
            return false;
        }
        if (pid == null || pid.isBlank()) {
            log.warn("易支付商户ID未配置");
            return false;
        }
        if (privateKey == null || privateKey.isBlank()) {
            log.warn("易支付 RSA 私钥未配置");
            return false;
        }

        try {
            // 尝试访问易支付 API 查询接口
            String testUrl = apiUrl + (apiUrl.endsWith("/") ? "" : "/") + "api.php?act=query&pid=" + urlEncode(pid);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("易支付连接测试成功, apiUrl={}", apiUrl);
                return true;
            } else {
                log.warn("易支付连接测试失败, statusCode={}", response.statusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("易支付连接测试异常", e);
            return false;
        }
    }

    @Override
    public String getSuccessResponse() {
        return "success";
    }

    // ========== 内部方法 ==========

    /**
     * 构造签名字符串：参数按 key ASCII 升序排序，用 & 连接 key=value
     * 空值参数不参与签名
     */
    private String buildSignString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    if (sb.length() > 0)
                        sb.append("&");
                    sb.append(e.getKey()).append("=").append(e.getValue());
                });
        return sb.toString();
    }

    /**
     * SHA256WithRSA 签名
     */
    private String rsaSign(String data, String privateKeyStr) throws Exception {
        // 去除 PEM 头尾和换行
        privateKeyStr = privateKeyStr
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * SHA256WithRSA 验签
     */
    private boolean rsaVerify(String data, String signBase64, String publicKeyStr) {
        try {
            publicKeyStr = publicKeyStr
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            log.error("RSA 验签异常", e);
            return false;
        }
    }

    /**
     * 解析易支付订单号（格式: u{userId}_a{articleId}_{timestamp}）
     */
    private void parseEpayOrderId(CallbackResult result, String outTradeNo) {
        if (outTradeNo == null || outTradeNo.isEmpty())
            return;
        try {
            // 格式: u42_a15_1708012345678
            String[] parts = outTradeNo.split("_");
            if (parts.length >= 2 && parts[0].startsWith("u") && parts[1].startsWith("a")) {
                result.setParsedUserId(Integer.parseInt(parts[0].substring(1)));
                result.setParsedArticleId(Integer.parseInt(parts[1].substring(1)));
            }
        } catch (NumberFormatException e) {
            log.warn("易支付订单号解析失败: {}", outTradeNo);
        }
    }

    private String getRequiredConfig(Map<String, Object> config, String key, String name) {
        String value = (String) config.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("易支付" + name + "(" + key + ")未配置");
        }
        return value;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
