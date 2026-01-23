package com.ld.poetry.config;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.ld.poetry.dao.HistoryInfoMapper;
import com.ld.poetry.dao.WebInfoMapper;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.SortMapper;
import com.ld.poetry.entity.*;
import com.ld.poetry.im.websocket.TioUtil;
import com.ld.poetry.im.websocket.TioWebsocketStarter;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.service.FamilyService;
import com.ld.poetry.service.UserService;
import com.ld.poetry.service.TranslationService;
import com.ld.poetry.utils.PrerenderClient;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.enums.PoetryEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class PoetryApplicationRunner implements ApplicationRunner {

    @Value("${store.type}")
    private String defaultType;

    @Autowired
    private WebInfoMapper webInfoMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private FamilyService familyService;

    @Autowired
    private HistoryInfoMapper historyInfoMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PrerenderClient prerenderClient;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SortMapper sortMapper;

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private com.ld.poetry.service.SitemapService sitemapService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private com.ld.poetry.service.SysAiConfigService sysAiConfigService;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor asyncExecutor;

    @Value("${prerender.startup.enabled:true}")
    private boolean prerenderStartupEnabled;

    @Value("${prerender.startup.delay:10}")
    private int prerenderStartupDelay;

    @Value("${prerender.startup.health-check.max-retries:5}")
    private int prerenderHealthCheckMaxRetries;

    @Value("${prerender.startup.health-check.base-delay:60}")
    private int prerenderHealthCheckBaseDelay;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 初始化网站信息缓存
        LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
        List<WebInfo> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            WebInfo webInfo = list.get(0);
            webInfo.setDefaultStoreType(defaultType);

            // 确保启用看板娘字段有默认值
            if (webInfo.getEnableWaifu() == null) {
                webInfo.setEnableWaifu(false);
            }

            // 确保status字段有默认值
            if (webInfo.getStatus() == null) {
                webInfo.setStatus(true);
                log.info("WebInfo status字段为null，设置为默认值true");
            }

            // 缓存网站信息到Redis（永久缓存）
            cacheService.cacheWebInfo(webInfo);

            log.info("网站基本信息已加载到Redis缓存(永久) - WebName: {}, EnableWaifu: {}, Status: {}",
                    webInfo.getWebName(), webInfo.getEnableWaifu(), webInfo.getStatus());
        } else {
            log.warn("未找到网站基本信息，请检查数据库");
        }

        // 初始化管理员用户缓存
        User admin = userService.lambdaQuery().eq(User::getUserType, PoetryEnum.USER_TYPE_ADMIN.getCode()).one();
        if (admin != null) {
            cacheService.cacheAdminUser(admin);
            log.info("管理员用户信息已加载到Redis缓存(永久) - Username: {}, ID: {}, Email: {}", 
                admin.getUsername(), admin.getId(), admin.getEmail());
        } else {
            log.error("未找到管理员用户，请检查数据库！应用可能无法正常工作");
        }

        // 初始化管理员家庭信息缓存
        if (admin != null) {
            Family family = familyService.lambdaQuery().eq(Family::getUserId, admin.getId()).one();
            if (family != null) {
                cacheService.cacheAdminFamily(family);
                log.info("管理员家庭信息已加载到缓存");
            }
        }

        // 初始化历史访问缓存
        List<HistoryInfo> infoList = new LambdaQueryChainWrapper<>(historyInfoMapper)
                .select(HistoryInfo::getIp, HistoryInfo::getUserId)
                .ge(HistoryInfo::getCreateTime, LocalDateTime.now().with(LocalTime.MIN))
                .list();

        cacheService.cacheIpHistory(new CopyOnWriteArraySet<>(infoList.stream().map(info -> info.getIp() + (info.getUserId() != null ? "_" + info.getUserId().toString() : "")).collect(Collectors.toList())));

        // 初始化访问统计缓存
        Map<String, Object> history = new HashMap<>();
        history.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
        history.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
        history.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
        history.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount());
        cacheService.cacheIpHistoryStatistics(history);

        // 初始化Tio
        TioUtil.buildTio();
        TioWebsocketStarter websocketStarter = TioUtil.getTio();
        if (websocketStarter != null) {
            websocketStarter.start();
        }

        // 启动时预热sitemap缓存
        try {
            if (sitemapService != null) {
                // 异步预热sitemap，避免阻塞应用启动（使用虚拟线程池）
                asyncExecutor.execute(() -> {
                    try {
                        Thread.sleep(5000); // 延迟5秒，确保应用完全启动
                        String sitemap = sitemapService.generateSitemap();
                        if (sitemap != null) {
                            int urlCount = sitemap.split("<url>").length - 1;
                            log.info("应用启动时sitemap预热成功，包含 {} 个URL", urlCount);
                        } else {
                            log.warn("应用启动时sitemap预热失败");
                        }
                    } catch (Exception e) {
                        log.warn("应用启动时sitemap预热失败，不影响应用启动", e);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("启动sitemap预热任务失败，不影响应用启动", e);
        }

        // 启动时自动预渲染
        if (prerenderStartupEnabled) {
            log.info("启动时预渲染已启用，将在{}秒后开始执行预渲染任务", prerenderStartupDelay);
            executeStartupPrerender();
        } else {
            log.info("启动时预渲染已禁用，跳过预渲染任务");
        }
    }

    /**
     * 执行启动时预渲染任务
     */
    private void executeStartupPrerender() {
        executeFullPrerender();
    }

    /**
     * 执行完整预渲染任务（可被外部调用）
     */
    public void executeFullPrerender() {
        // 使用虚拟线程异步执行预渲染，避免阻塞应用启动
        Thread.ofVirtual().name("startup-prerender-virtual").start(() -> {
            try {
                // 延迟执行，确保应用完全启动
                Thread.sleep(prerenderStartupDelay * 1000L);
                
                log.info("开始执行启动时预渲染任务...");
                
                // 1. 检查预渲染服务健康状态（带退避策略）
                if (!checkPrerenderHealthWithRetry()) {
                    log.warn("预渲染服务健康检查最终失败，跳过预渲染任务");
                    return;
                }
                
                // 2. 预渲染主要页面（首页、百宝箱、分类索引）
                renderMainPages();
                
                // 3. 预渲染所有分类详情页面
                renderAllCategoryPages();
                
                // 4. 预渲染所有已发布的文章
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
     * @return true 如果健康检查通过，false 如果所有重试都失败
     */
    private boolean checkPrerenderHealthWithRetry() {
        for (int attempt = 1; attempt <= prerenderHealthCheckMaxRetries; attempt++) {
            try {
                prerenderClient.checkHealth();
                log.info("预渲染服务健康检查通过（第{}次尝试）", attempt);
                return true;
            } catch (Exception e) {
                log.warn("预渲染服务健康检查失败（第{}/{}次尝试）: {}", 
                    attempt, prerenderHealthCheckMaxRetries, e.getMessage());
                
                // 如果不是最后一次尝试，则等待后重试
                if (attempt < prerenderHealthCheckMaxRetries) {
                    // 计算退避延迟时间：第1次失败等待1分钟，第2次等待3分钟，第3次等待5分钟，第4次等待7分钟
                    int delayMinutes = prerenderHealthCheckBaseDelay + (attempt - 1) * 120; // 基础60秒 + 递增120秒
                    int delaySeconds = delayMinutes;
                    
                    log.info("预渲染服务暂不可用，{}秒后进行第{}次重试...", delaySeconds, attempt + 1);
                    
                    try {
                        Thread.sleep(delaySeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("预渲染服务健康检查重试被中断");
                        return false;
                    }
                } else {
                    log.error("预渲染服务健康检查失败，已达到最大重试次数（{}次）", prerenderHealthCheckMaxRetries);
                }
            }
        }
        return false;
    }

    /**
     * 预渲染主要页面
     */
    private void renderMainPages() {
        try {
            log.info("开始预渲染主要页面...");
            
            // 预渲染所有静态页面（包含首页、百宝箱等）
            prerenderClient.renderAllStaticPages();
            Thread.sleep(1000);
            
            // 预渲染分类索引页面
            prerenderClient.renderSortIndexPage();
            
            log.info("主要页面预渲染完成");
        } catch (Exception e) {
            log.error("主要页面预渲染失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预渲染所有分类详情页面
     */
    private void renderAllCategoryPages() {
        try {
            log.info("开始预渲染所有分类详情页面...");
            
            // 获取所有分类ID
            List<Sort> sorts = new LambdaQueryChainWrapper<>(sortMapper).list();
            if (!CollectionUtils.isEmpty(sorts)) {
                List<Integer> sortIds = sorts.stream()
                    .map(Sort::getId)
                    .collect(Collectors.toList());
                
                log.info("找到{}个分类，开始批量预渲染", sortIds.size());
                
                // 批量预渲染所有分类页面
                prerenderClient.renderAllCategoryPages(sortIds);
                
                log.info("所有分类详情页面预渲染完成，共{}个分类", sortIds.size());
            } else {
                log.info("未找到任何分类，跳过分类页面预渲染");
            }
        } catch (Exception e) {
            log.error("分类详情页面预渲染失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预渲染所有已发布的文章
     */
    private void renderAllPublishedArticles() {
        try {
            log.info("开始预渲染所有已发布的文章...");     
            // 获取所有已发布的文章ID
            List<Article> articles = new LambdaQueryChainWrapper<>(articleMapper)
                .select(Article::getId)
                .eq(Article::getViewStatus, PoetryEnum.PUBLIC.getCode()) // 只预渲染公开文章
                .orderByDesc(Article::getCreateTime) // 按创建时间倒序，优先渲染最新文章
                .list();
            
            if (!CollectionUtils.isEmpty(articles)) {
                List<Integer> articleIds = articles.stream()
                    .map(Article::getId)
                    .collect(Collectors.toList());
                
                log.info("找到{}篇已发布文章，开始分批预渲染", articleIds.size());
                
                // 分批处理，避免一次性提交过多文章导致超时
                int batchSize = 10; // 减少批次大小，因为需要查询每篇文章的可用语言
                int totalBatches = (articleIds.size() + batchSize - 1) / batchSize;
                
                for (int i = 0; i < totalBatches; i++) {
                    int startIndex = i * batchSize;
                    int endIndex = Math.min(startIndex + batchSize, articleIds.size());
                    List<Integer> batchIds = articleIds.subList(startIndex, endIndex);
                    
                    log.info("预渲染第{}/{}批文章，包含{}篇文章", i + 1, totalBatches, batchIds.size());
                    
                    try {
                        // 为每篇文章获取可用翻译语言并渲染
                        renderArticlesWithAvailableLanguages(batchIds);
                        
                        // 批次间延迟，避免对预渲染服务造成过大压力
                        if (i < totalBatches - 1) {
                            Thread.sleep(3000); // 增加批次间延迟到3秒
                        }
                    } catch (Exception e) {
                        log.warn("第{}/{}批文章预渲染失败: {}", i + 1, totalBatches, e.getMessage());
                        // 继续处理下一批，不因单批失败而中断整个流程
                    }
                }
                
                log.info("所有已发布文章预渲染完成，共{}篇文章，分{}批处理", articleIds.size(), totalBatches);
            } else {
                log.info("未找到任何已发布文章，跳过文章预渲染");
            }
        } catch (Exception e) {
            log.error("文章预渲染失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 为文章列表渲染所有可用语言版本
     * @param articleIds 文章ID列表
     */
    private void renderArticlesWithAvailableLanguages(List<Integer> articleIds) {
        for (Integer articleId : articleIds) {
            try {
                // 获取该文章的可用翻译语言（不包含源语言）
                List<String> translationLanguages = translationService.getArticleAvailableLanguages(articleId);
                
                if (!translationLanguages.isEmpty()) {
                    // 如果有翻译语言，需要构建完整的渲染语言列表（源语言 + 翻译语言）
                    Map<String, Object> defaultLangs = sysAiConfigService.getDefaultLanguages();
                    String sourceLanguage = defaultLangs != null ? 
                        (String) defaultLangs.getOrDefault("default_source_lang", "zh") : "zh";
                    
                    // 构建完整的渲染语言列表
                    List<String> allLanguagesToRender = new ArrayList<>();
                    if (sourceLanguage != null && !sourceLanguage.trim().isEmpty()) {
                        allLanguagesToRender.add(sourceLanguage); // 添加源语言
                    }
                    allLanguagesToRender.addAll(translationLanguages); // 添加翻译语言
                    
                    log.info("文章{}将渲染多语言版本，源语言: {}, 翻译语言: {}, 完整渲染列表: {}", 
                            articleId, sourceLanguage, translationLanguages, allLanguagesToRender);
                    prerenderClient.renderArticleWithLanguages(articleId, allLanguagesToRender);
                } else {
                    // 如果没有翻译语言，只渲染源语言版本
                    log.info("文章{}只渲染源语言版本", articleId);
                    prerenderClient.renderArticle(articleId);
                }
                
                // 文章间延迟，避免过于频繁的API调用
                Thread.sleep(500);
                
            } catch (Exception e) {
                log.warn("文章{}预渲染失败: {}", articleId, e.getMessage());
                // 继续处理下一篇文章
            }
        }
    }
}
