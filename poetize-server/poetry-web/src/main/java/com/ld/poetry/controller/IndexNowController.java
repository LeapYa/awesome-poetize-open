package com.ld.poetry.controller;

import com.ld.poetry.service.SeoConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * IndexNow Key 验证文件控制器
 *
 * <p>IndexNow 协议要求网站在根目录下放置 {api-key}.txt 文件，
 * 搜索引擎（Bing、Yandex 等）在收到推送请求后会回调该文件以验证所有权。</p>
 *
 * <p>本控制器根据 SEO 配置中的 bing_api_key 动态返回验证文件内容，
 * 无需手动在服务器上创建静态文件。</p>
 *
 * @author LeapYa
 * @since 2026-03-30
 * @see <a href="https://www.indexnow.org/documentation">IndexNow Documentation</a>
 */
@Slf4j
@RestController
public class IndexNowController {

    @Autowired
    private SeoConfigService seoConfigService;

    /**
     * IndexNow Key 合法性校验正则
     * 根据 IndexNow 官方规范：8-128 位，仅包含 a-z A-Z 0-9 和 -
     */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]{8,128}$");

    /**
     * 动态返回 IndexNow 验证文件内容
     *
     * <p>当搜索引擎（Bing/Yandex 等）收到 IndexNow 推送请求后，
     * 会访问 https://域名/{key}.txt 来验证网站所有权。
     * 此端点会检查请求的 key 是否与 SEO 配置中的 bing_api_key 匹配，
     * 若匹配则返回纯文本形式的 key，否则返回 404。</p>
     *
     * @param key 请求的 IndexNow key（不含 .txt 后缀）
     * @return 纯文本形式的 key 内容或 404
     */
    @GetMapping(value = "/{key}.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getIndexNowKeyFile(@PathVariable("key") String key) {
        // 基本格式校验：防止路径遍历和非法请求
        if (!StringUtils.hasText(key) || !KEY_PATTERN.matcher(key).matches()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Map<String, Object> seoConfig = seoConfigService.getSeoConfigAsJson();
            if (seoConfig == null || seoConfig.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 检查是否匹配 Bing/IndexNow API Key
            String bingApiKey = (String) seoConfig.get("bing_api_key");

            if (StringUtils.hasText(bingApiKey) && bingApiKey.equals(key)) {
                log.info("IndexNow key 验证文件请求命中: key={}", key);
                return ResponseEntity.ok()
                        .header("Cache-Control", "public, max-age=86400") // 缓存24小时
                        .header("X-Robots-Tag", "noindex") // 阻止搜索引擎索引此文件本身
                        .body(key);
            }

            // 也检查 Yandex 的 API Key（Yandex 同样支持 IndexNow）
            String yandexApiKey = (String) seoConfig.get("yandex_api_key");
            if (StringUtils.hasText(yandexApiKey) && yandexApiKey.equals(key)) {
                log.info("IndexNow key 验证文件请求命中(Yandex): key={}", key);
                return ResponseEntity.ok()
                        .header("Cache-Control", "public, max-age=86400")
                        .header("X-Robots-Tag", "noindex")
                        .body(key);
            }

            // key 不匹配，返回 404
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("处理 IndexNow key 验证文件请求失败: key={}", key, e);
            return ResponseEntity.notFound().build();
        }
    }
}
