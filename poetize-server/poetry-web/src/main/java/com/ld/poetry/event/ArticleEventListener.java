package com.ld.poetry.event;

import com.ld.poetry.service.SitemapService;
import com.ld.poetry.service.TranslationService;
import com.ld.poetry.service.SysAiConfigService;
import com.ld.poetry.service.SeoService;
import com.ld.poetry.service.ArticleService;
import com.ld.poetry.utils.PrerenderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.StructuredTaskScope;

/**
 * 文章事件监听器
 * 在数据库事务提交后执行预渲染操作
 */
@Component
@Slf4j
public class ArticleEventListener {
    
    @Autowired
    private PrerenderClient prerenderClient;
    
    @Autowired
    private SitemapService sitemapService;
    
    @Autowired
    private TranslationService translationService;
    
    @Autowired
    private SysAiConfigService sysAiConfigService;
    
    @Autowired
    private SeoService seoService;
    
    @org.springframework.context.annotation.Lazy
    @Autowired
    private ArticleService articleService;
    
    // 用于去重的延迟调度器（使用虚拟线程工厂）
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, 
        Thread.ofVirtual().name("article-event-scheduler-", 0).factory());
    
    // 记录待处理的预渲染任务，用于去重
    private final ConcurrentHashMap<Integer, Runnable> pendingRenderTasks = new ConcurrentHashMap<>();
    
    /**
     * 监听文章保存事件，在事务提交后执行预渲染
     * 
     * 注意：使用fallbackExecution=true确保即使在非事务上下文（如虚拟线程）中发布事件也能触发
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Async
    public void handleArticleSavedEvent(ArticleSavedEvent event) {
        log.info("收到文章事件: ID={}, 操作={}, 可见={}, 分类ID={}, 提交搜索引擎={}", 
                 event.getArticleId(), event.getOperationType(), event.getViewStatus(), 
                 event.getSortId(), event.getSubmitToSearchEngine());
        
        try {
            switch (event.getOperationType()) {
                case "CREATE":
                    if (Boolean.TRUE.equals(event.getViewStatus())) {
                        // 新建可见文章，使用延迟去重机制进行预渲染
                        scheduleRenderWithDeduplication(event.getArticleId(), event.getSortId(), "CREATE", 
                                event.getSubmitToSearchEngine(), event.getViewStatus(), event.getTaskId());
                        log.info("已安排新文章预渲染任务: ID={}", event.getArticleId());
                        
                        // 文章创建后更新sitemap
                        updateSitemapAsync(event.getArticleId(), "CREATE");
                    } else {
                        // 新建不可见文章，不需要预渲染
                        log.info("新建文章不可见，跳过预渲染: ID={}", event.getArticleId());
                    }
                    break;
                    
                case "UPDATE":
                    if (Boolean.TRUE.equals(event.getViewStatus())) {
                        // 文章可见，使用延迟去重机制进行预渲染
                        scheduleRenderWithDeduplication(event.getArticleId(), event.getSortId(), "RENDER", 
                                event.getSubmitToSearchEngine(), event.getViewStatus(), event.getTaskId());
                        log.info("已安排文章更新预渲染任务: ID={}", event.getArticleId());
                    } else {
                        // 文章不可见，删除预渲染文件
                        scheduleRenderWithDeduplication(event.getArticleId(), event.getSortId(), "DELETE", 
                                null, null, event.getTaskId());
                        log.info("已安排文章预渲染删除任务: ID={}", event.getArticleId());
                    }
                    
                    // 文章更新后更新sitemap
                    updateSitemapAsync(event.getArticleId(), "UPDATE");
                    break;
                    
                case "DELETE":
                    // 删除操作立即执行，不需要去重
                    scheduleRenderWithDeduplication(event.getArticleId(), event.getSortId(), "DELETE", 
                            null, null, event.getTaskId());
                    log.info("已安排文章删除预渲染任务: ID={}, 分类ID={}", event.getArticleId(), event.getSortId());
                    
                    // 文章删除后更新sitemap
                    updateSitemapAsync(event.getArticleId(), "DELETE");
                    break;
                    
                default:
                    log.warn("未知的文章操作类型: {}", event.getOperationType());
            }
        } catch (Exception e) {
            log.warn("文章预渲染任务安排失败: ID={}, 操作={}, 错误={}", 
                     event.getArticleId(), event.getOperationType(), e.getMessage());
        }
    }
    
    /**
     * 去重调度预渲染任务
     * 如果短时间内同一文章有多次渲染请求，只执行最后一次
     */
    private void scheduleRenderWithDeduplication(Integer articleId, Integer sortId, String action, Boolean submitToSearchEngine, Boolean viewStatus, String taskId) {
        Runnable renderTask = () -> {
            try {
                // 从待处理任务中移除
                pendingRenderTasks.remove(articleId);
                
                if ("CREATE".equals(action)) {
                    // 新建文章的预渲染处理
                    // 先清理预渲染缓存，确保新文章能正常访问
                    prerenderClient.clearPrerenderCache();
                    
                    // 使用并行预渲染，提升速度
                    executeParallelPrerender(articleId, sortId, "CREATE");
                    
                    
                    // 预渲染完成后，如果需要推送到搜索引擎，则执行SEO推送
                    if (Boolean.TRUE.equals(submitToSearchEngine) && Boolean.TRUE.equals(viewStatus)) {
                        performSeoSubmission(articleId, "CREATE", taskId);
                    }
                    
                } else if ("RENDER".equals(action)) {
                    // 更新文章的预渲染处理
                    // 使用并行预渲染，提升速度
                    executeParallelPrerender(articleId, sortId, "UPDATE");
                    
                    
                    // 预渲染完成后，如果需要推送到搜索引擎，则执行SEO推送
                    if (Boolean.TRUE.equals(submitToSearchEngine) && Boolean.TRUE.equals(viewStatus)) {
                        performSeoSubmission(articleId, "UPDATE", taskId);
                    }
                    
                } else if ("DELETE".equals(action)) {
                    // 执行删除
                    prerenderClient.deleteArticle(articleId);
                    prerenderClient.clearPrerenderCache();
                    
                    // 并行重新渲染相关页面
                    try (var scope = StructuredTaskScope.open()) {
                        log.info("开始并行预渲染（删除后），文章ID={}", articleId);
                        
                        // Fork 首页预渲染
                        scope.fork(() -> {
                            prerenderClient.renderHomePage();
                            return "home-done";
                        });
                        
                        // Fork 分类索引页预渲染
                        scope.fork(() -> {
                            prerenderClient.renderSortIndexPage();
                            return "sortIndex-done";
                        });
                        
                        // Fork 分类页预渲染（如果有分类ID）
                        if (sortId != null) {
                            scope.fork(() -> {
                                prerenderClient.renderCategoryPage(sortId);
                                return "category-done";
                            });
                        }
                        
                        // 等待所有任务完成
                        scope.join();
                        
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("删除后并行预渲染被中断: 文章ID={}", articleId, e);
                    } catch (Exception e) {
                        log.error("删除后并行预渲染失败: 文章ID={}", articleId, e);
                    }
                }
            } catch (Exception e) {
                log.warn("预渲染执行失败: ID={}, 操作={}, 错误={}", 
                         articleId, action, e.getMessage());
            }
        };
        
        // 如果已经有待处理的任务，先取消之前的任务
        Runnable previousTask = pendingRenderTasks.put(articleId, renderTask);
        if (previousTask != null) {
            log.info("发现重复预渲染任务，已取消前一个任务: ID={}", articleId);
        }
        
        // 立即执行预渲染（不延迟），因为翻译在发布事件前就已经保存完成
        scheduler.schedule(renderTask, 0, TimeUnit.SECONDS);
    }
    
    /**
     * 异步更新sitemap缓存（文章变更时只清除缓存，不推送）
     * 
     * @param articleId 文章ID
     * @param operation 操作类型
     */
    private void updateSitemapAsync(Integer articleId, String operation) {
        try {
            log.info("文章{}操作，清除sitemap缓存: ID={}", operation, articleId);
            sitemapService.updateArticleSitemap(articleId);
        } catch (Exception e) {
            log.warn("清除sitemap缓存失败: ID={}, 操作={}, 错误={}", 
                     articleId, operation, e.getMessage());
        }
    }
    
    /**
     * 获取文章的所有可用语言（源语言 + 翻译语言）
     * 
     * @param articleId 文章ID
     * @return 所有可用语言列表
     */
    private List<String> getAllAvailableLanguages(Integer articleId) {
        List<String> allLanguages = new ArrayList<>();
        
        try {
            // 1. 获取源语言（默认中文）
            Map<String, Object> defaultLangs = sysAiConfigService.getDefaultLanguages();
            String sourceLanguage = defaultLangs != null ? 
                (String) defaultLangs.getOrDefault("default_source_lang", "zh") : "zh";
            
            if (sourceLanguage != null && !sourceLanguage.trim().isEmpty()) {
                allLanguages.add(sourceLanguage);
            }
            
            // 2. 获取所有翻译语言
            List<String> translationLanguages = translationService.getArticleAvailableLanguages(articleId);
            if (translationLanguages != null && !translationLanguages.isEmpty()) {
                allLanguages.addAll(translationLanguages);
            }
            
            log.info("文章ID {} 的可用语言: 源语言={}, 翻译语言={}, 完整列表={}", 
                    articleId, sourceLanguage, translationLanguages, allLanguages);
            
        } catch (Exception e) {
            log.warn("获取文章可用语言失败，使用默认中文，文章ID: {}, 错误: {}", articleId, e.getMessage());
            // 如果获取失败，至少保证有中文
            if (allLanguages.isEmpty()) {
                allLanguages.add("zh");
            }
        }
        
        return allLanguages;
    }
    
    /**
     * 执行SEO推送（预渲染完成后立即执行）
     * 向搜索引擎推送新增/更新的文章并记录状态
     * 
     * @param articleId 文章ID
     * @param operation 操作类型
     * @param taskId 异步任务ID（如果有）
     */
    private void performSeoSubmission(Integer articleId, String operation, String taskId) {
        try {
            if (taskId != null) {
                articleService.updateSeoPushStatus(taskId, "pushing", "正在推送到搜索引擎...");
            }
            
            Map<String, Object> result = seoService.submitToSearchEngines(articleId);
            String status = (String) result.get("status");
            String message = (String) result.get("message");
            
            // 写入任务状态（让前端SSE流可以感知到SEO推送结果）
            if (taskId != null) {
                // 如果是"skipped"或"disabled"，前端一般显示为完成状态（无需红色警告）
                // 我们在 ArticleServiceImpl 中写回
                String pushStatus = "pushed".equals(status) ? "success" 
                                  : "skipped".equals(status) || "disabled".equals(status) ? "skipped"
                                  : "failed";
                articleService.updateSeoPushStatus(taskId, pushStatus, message);
            }
            
            // 根据不同状态输出不同的日志
            switch (status) {
                case "pushed":
                    log.info("搜索引擎推送完成，文章ID: {}, {}", articleId, message);
                    break;
                case "skipped":
                    log.info("搜索引擎推送跳过，文章ID: {}, 原因: {}", articleId, message);
                    break;
                case "disabled":
                    break;
                case "failed":
                    log.warn("搜索引擎推送失败，文章ID: {}, {}", articleId, message);
                    break;
                case "error":
                    log.error("搜索引擎推送错误，文章ID: {}, {}", articleId, message);
                    if (taskId != null) {
                        articleService.updateSeoPushStatus(taskId, "failed", message);
                    }
                    break;
                default:
                    log.info("搜索引擎推送完成，文章ID: {}, 状态: {}", articleId, status);
            }
        } catch (Exception e) {
            log.error("搜索引擎推送异常，文章ID: {}, 错误: {}", articleId, e.getMessage(), e);
            if (taskId != null) {
                articleService.updateSeoPushStatus(taskId, "failed", "推送执行异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 并行执行预渲染任务，大幅提升速度
     * 
     * @param articleId 文章ID
     * @param sortId 分类ID
     * @param operation 操作类型
     */
    private void executeParallelPrerender(Integer articleId, Integer sortId, String operation) {
        try {
            // 获取文章的所有可用语言
            List<String> allLanguages = getAllAvailableLanguages(articleId);
            
            // 使用StructuredTaskScope并行执行所有预渲染任务
            try (var scope = StructuredTaskScope.open()) {
                log.info("开始并行预渲染，文章ID={}, 操作={}", articleId, operation);
                
                // Fork 文章页多语言预渲染
                scope.fork(() -> {
                    prerenderClient.renderArticleWithLanguages(articleId, allLanguages);
                    return "article-done";
                });
                
                // Fork 首页预渲染
                scope.fork(() -> {
                    prerenderClient.renderHomePage();
                    return "home-done";
                });
                
                // Fork 分类索引页预渲染
                scope.fork(() -> {
                    prerenderClient.renderSortIndexPage();
                    return "sortIndex-done";
                });
                
                // Fork 分类页预渲染（如果有分类ID）
                if (sortId != null) {
                    scope.fork(() -> {
                        prerenderClient.renderCategoryPage(sortId);
                        return "category-done";
                    });
                }
                
                // 等待所有任务完成
                scope.join();
                
                log.info("所有预渲染任务并行完成: 文章ID={}, 操作={}", articleId, operation);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("并行预渲染被中断: 文章ID={}, 操作={}", articleId, operation, e);
                throw new RuntimeException("预渲染被中断", e);
            } catch (Exception e) {
                log.error("并行预渲染失败: 文章ID={}, 操作={}", articleId, operation, e);
                throw new RuntimeException("预渲染失败", e);
            }
            
        } catch (Exception e) {
            log.error("执行并行预渲染失败: 文章ID={}, 操作={}", articleId, operation, e);
            throw e;
        }
    }
} 
