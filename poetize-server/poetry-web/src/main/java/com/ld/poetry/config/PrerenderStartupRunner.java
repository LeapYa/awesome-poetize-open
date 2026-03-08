package com.ld.poetry.config;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.SortMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.Sort;
import com.ld.poetry.enums.PoetryEnum;
import com.ld.poetry.service.TranslationService;
import com.ld.poetry.utils.PrerenderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 启动时预渲染 Runner
 *
 * <p>
 * 应用启动后异步预渲染主要页面、分类页面和文章页面，
 * 保证搜索引擎爬虫和社交媒体抓取时能获取完整的 HTML。
 *
 * <p>
 * 从 {@link PoetryApplicationRunner} 拆分出来，遵循单一职责原则。
 *
 * @author LeapYa
 * @since 2026-03-05
 */
@Component
@Order(30)
@Slf4j
public class PrerenderStartupRunner implements ApplicationRunner {

    @Autowired
    private PrerenderClient prerenderClient;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SortMapper sortMapper;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private com.ld.poetry.service.SysAiConfigService sysAiConfigService;

    @Value("${prerender.startup.enabled:true}")
    private boolean prerenderStartupEnabled;

    @Value("${prerender.startup.delay:10}")
    private int prerenderStartupDelay;

    @Value("${prerender.startup.health-check.max-retries:5}")
    private int prerenderHealthCheckMaxRetries;

    @Value("${prerender.startup.health-check.base-delay:60}")
    private int prerenderHealthCheckBaseDelay;

    @Override
    public void run(ApplicationArguments args) {
        if (!prerenderStartupEnabled) {
            log.info("启动时预渲染已禁用，跳过预渲染任务");
            return;
        }

        log.info("启动时预渲染已启用，将在 {} 秒后开始执行", prerenderStartupDelay);
        executeFullPrerender();
    }

    /**
     * 执行完整预渲染任务（也可被外部调用，如管理员手动触发）
     */
    public void executeFullPrerender() {
        Thread.ofVirtual().name("startup-prerender-virtual").start(() -> {
            try {
                Thread.sleep(prerenderStartupDelay * 1000L);
                log.info("开始执行启动时预渲染任务...");

                if (!checkPrerenderHealthWithRetry()) {
                    log.warn("预渲染服务健康检查最终失败，跳过预渲染任务");
                    return;
                }

                renderMainPages();
                renderAllCategoryPages();
                renderAllPublishedArticles();

                log.info("启动时预渲染任务执行完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("启动时预渲染任务被中断: {}", e.getMessage());
            } catch (Exception e) {
                log.error("启动时预渲染任务执行失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 带退避策略的预渲染服务健康检查
     */
    private boolean checkPrerenderHealthWithRetry() {
        for (int attempt = 1; attempt <= prerenderHealthCheckMaxRetries; attempt++) {
            try {
                prerenderClient.checkHealth();
                log.info("预渲染服务健康检查通过（第 {} 次尝试）", attempt);
                return true;
            } catch (Exception e) {
                log.warn("预渲染服务健康检查失败（第 {}/{} 次尝试）: {}",
                        attempt, prerenderHealthCheckMaxRetries, e.getMessage());

                if (attempt < prerenderHealthCheckMaxRetries) {
                    int delaySeconds = prerenderHealthCheckBaseDelay + (attempt - 1) * 120;
                    log.info("预渲染服务暂不可用，{} 秒后进行第 {} 次重试...", delaySeconds, attempt + 1);
                    try {
                        Thread.sleep(delaySeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("预渲染服务健康检查重试被中断");
                        return false;
                    }
                } else {
                    log.error("预渲染服务健康检查失败，已达到最大重试次数（{} 次）", prerenderHealthCheckMaxRetries);
                }
            }
        }
        return false;
    }

    private void renderMainPages() {
        try {
            log.info("开始预渲染主要页面...");
            prerenderClient.renderAllStaticPages();
            Thread.sleep(1000);
            prerenderClient.renderSortIndexPage();
            log.info("主要页面预渲染完成");
        } catch (Exception e) {
            log.error("主要页面预渲染失败: {}", e.getMessage(), e);
        }
    }

    private void renderAllCategoryPages() {
        try {
            log.info("开始预渲染所有分类详情页面...");
            List<Sort> sorts = new LambdaQueryChainWrapper<>(sortMapper).list();
            if (!CollectionUtils.isEmpty(sorts)) {
                List<Integer> sortIds = sorts.stream().map(Sort::getId).collect(Collectors.toList());
                log.info("找到 {} 个分类，开始批量预渲染", sortIds.size());
                prerenderClient.renderAllCategoryPages(sortIds);
                log.info("所有分类详情页面预渲染完成，共 {} 个分类", sortIds.size());
            } else {
                log.info("未找到任何分类，跳过分类页面预渲染");
            }
        } catch (Exception e) {
            log.error("分类详情页面预渲染失败: {}", e.getMessage(), e);
        }
    }

    private void renderAllPublishedArticles() {
        try {
            log.info("开始预渲染所有已发布的文章...");
            List<Article> articles = new LambdaQueryChainWrapper<>(articleMapper)
                    .select(Article::getId)
                    .eq(Article::getViewStatus, PoetryEnum.PUBLIC.getCode())
                    .orderByDesc(Article::getCreateTime)
                    .list();

            if (CollectionUtils.isEmpty(articles)) {
                log.info("未找到任何已发布文章，跳过文章预渲染");
                return;
            }

            List<Integer> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());
            log.info("找到 {} 篇已发布文章，开始分批预渲染", articleIds.size());

            int batchSize = 10;
            int totalBatches = (articleIds.size() + batchSize - 1) / batchSize;

            for (int i = 0; i < totalBatches; i++) {
                int startIndex = i * batchSize;
                int endIndex = Math.min(startIndex + batchSize, articleIds.size());
                List<Integer> batchIds = articleIds.subList(startIndex, endIndex);
                log.info("预渲染第 {}/{} 批文章，包含 {} 篇", i + 1, totalBatches, batchIds.size());

                try {
                    renderArticlesWithAvailableLanguages(batchIds);
                    if (i < totalBatches - 1) {
                        Thread.sleep(3000);
                    }
                } catch (Exception e) {
                    log.warn("第 {}/{} 批文章预渲染失败: {}", i + 1, totalBatches, e.getMessage());
                }
            }
            log.info("所有已发布文章预渲染完成，共 {} 篇，分 {} 批处理", articleIds.size(), totalBatches);
        } catch (Exception e) {
            log.error("文章预渲染失败: {}", e.getMessage(), e);
        }
    }

    private void renderArticlesWithAvailableLanguages(List<Integer> articleIds) {
        for (Integer articleId : articleIds) {
            try {
                List<String> translationLanguages = translationService.getArticleAvailableLanguages(articleId);

                if (!translationLanguages.isEmpty()) {
                    Map<String, Object> defaultLangs = sysAiConfigService.getDefaultLanguages();
                    String sourceLanguage = defaultLangs != null
                            ? (String) defaultLangs.getOrDefault("default_source_lang", "zh")
                            : "zh";

                    List<String> allLanguagesToRender = new ArrayList<>();
                    if (sourceLanguage != null && !sourceLanguage.trim().isEmpty()) {
                        allLanguagesToRender.add(sourceLanguage);
                    }
                    allLanguagesToRender.addAll(translationLanguages);

                    log.info("文章 {} 将渲染多语言版本，源语言: {}, 翻译语言: {}", articleId, sourceLanguage, translationLanguages);
                    prerenderClient.renderArticleWithLanguages(articleId, allLanguagesToRender);
                } else {
                    log.info("文章 {} 只渲染源语言版本", articleId);
                    prerenderClient.renderArticle(articleId);
                }

                Thread.sleep(500);
            } catch (Exception e) {
                log.warn("文章 {} 预渲染失败: {}", articleId, e.getMessage());
            }
        }
    }
}
