package com.ld.poetry.config;

import com.ld.poetry.service.SitemapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * Sitemap 预热 Runner
 *
 * <p>
 * 应用启动后异步预热 sitemap 缓存，避免首次访问时的冷启动延迟。
 * 延迟 5 秒执行，确保应用完全启动后再生成。
 *
 * <p>
 * 从 {@link PoetryApplicationRunner} 拆分出来，遵循单一职责原则。
 *
 * @author LeapYa
 * @since 2026-03-05
 */
@Component
@Order(20)
@Slf4j
public class SitemapWarmupRunner implements ApplicationRunner {

    @Autowired(required = false)
    private SitemapService sitemapService;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor asyncExecutor;

    @Override
    public void run(ApplicationArguments args) {
        if (sitemapService == null) {
            log.info("SitemapService 未注入，跳过 sitemap 预热");
            return;
        }

        asyncExecutor.execute(() -> {
            try {
                Thread.sleep(5000); // 延迟 5 秒，确保应用完全启动
                String sitemap = sitemapService.generateSitemap();
                if (sitemap != null) {
                    int urlCount = sitemap.split("<url>").length - 1;
                    log.info("Sitemap 预热成功，包含 {} 个 URL", urlCount);
                } else {
                    log.warn("Sitemap 预热失败（返回 null）");
                }
            } catch (Exception e) {
                log.warn("Sitemap 预热失败，不影响应用启动", e);
            }
        });
    }
}
