package com.ld.poetry.service.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ld.poetry.entity.SysAiConfig;
import com.ld.poetry.service.SysAiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Random;

/**
 * 百度翻译 API 直接调用
 * 替代 Python 端的 _baidu_translate 方法
 *
 * 百度通用翻译 API: https://fanyi-api.baidu.com/api/trans/vip/translate
 * 签名算法: MD5(appid + query + salt + secret)
 */
@Slf4j
@Service
public class BaiduTranslationProvider {

    private static final String BAIDU_API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    private static final Random RANDOM = new Random();

    @Autowired
    private SysAiConfigService sysAiConfigService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 使用百度翻译 API 翻译文本
     *
     * @param text       待翻译文本
     * @param sourceLang 源语言代码 (auto/zh/en/ja 等)
     * @param targetLang 目标语言代码
     * @return 翻译后文本，失败返回 null
     */
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }

        try {
            // 从数据库获取百度翻译配置
            SysAiConfig config = sysAiConfigService.getArticleAiConfigInternal("default");
            if (config == null || config.getBaiduConfig() == null) {
                log.error("百度翻译配置未找到");
                return null;
            }

            JSONObject baiduConfig = JSON.parseObject(config.getBaiduConfig());
            String appId = baiduConfig.getString("app_id");
            String appSecret = baiduConfig.getString("app_secret");

            if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
                log.error("百度翻译 app_id 或 app_secret 未配置");
                return null;
            }

            return callBaiduApi(text, sourceLang, targetLang, appId, appSecret);

        } catch (Exception e) {
            log.error("百度翻译失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 使用指定的 appId/appSecret 调用百度翻译（用于测试连接）
     */
    public String translateWithConfig(String text, String sourceLang, String targetLang,
            String appId, String appSecret) {
        return callBaiduApi(text, sourceLang, targetLang, appId, appSecret);
    }

    /**
     * 调用百度翻译 API
     */
    @SuppressWarnings("unchecked")
    private String callBaiduApi(String text, String sourceLang, String targetLang,
            String appId, String appSecret) {
        try {
            String fromLang = (sourceLang == null || "auto".equals(sourceLang)) ? "auto" : sourceLang;
            String salt = String.valueOf(RANDOM.nextInt(32768, 65536));

            // 生成签名: MD5(appid + query + salt + secret)
            String signStr = appId + text + salt + appSecret;
            String sign = md5(signStr);

            // 构建表单参数
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("q", text);
            formData.add("from", fromLang);
            formData.add("to", targetLang);
            formData.add("appid", appId);
            formData.add("salt", salt);
            formData.add("sign", sign);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    BAIDU_API_URL, request, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                if (body.containsKey("trans_result")) {
                    var transResult = (java.util.List<Map<String, String>>) body.get("trans_result");
                    if (transResult != null && !transResult.isEmpty()) {
                        String translated = transResult.get(0).get("dst");
                        log.info("百度翻译成功: {} -> {}", fromLang, targetLang);
                        return translated;
                    }
                } else {
                    String errorCode = String.valueOf(body.get("error_code"));
                    String errorMsg = String.valueOf(body.get("error_msg"));
                    log.error("百度翻译 API 错误: code={}, msg={}", errorCode, errorMsg);
                }
            }

            return null;

        } catch (Exception e) {
            log.error("百度翻译 API 调用失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 计算 MD5 哈希值（小写十六进制）
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }
    }
}
