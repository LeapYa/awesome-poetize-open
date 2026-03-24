package com.ld.poetry.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.Article;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ld.poetry.vo.ArticleVO;
import com.ld.poetry.vo.BaseRequestVO;
import com.ld.poetry.service.impl.ArticleServiceImpl.ArticleSaveStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 文章表 服务类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
public interface ArticleService extends IService<Article> {

    PoetryResult saveArticle(ArticleVO articleVO);
    
    PoetryResult saveArticle(ArticleVO articleVO, boolean skipAiTranslation, Map<String, String> pendingTranslation);

    PoetryResult deleteArticle(Integer id);

    PoetryResult updateArticle(ArticleVO articleVO);
    
    PoetryResult updateArticle(ArticleVO articleVO, boolean skipAiTranslation, Map<String, String> pendingTranslation);

    PoetryResult<Page> listArticle(BaseRequestVO baseRequestVO);

    PoetryResult<ArticleVO> getArticleById(Integer id, String password);

    PoetryResult<Page> listAdminArticle(BaseRequestVO baseRequestVO, Boolean isBoss);

    PoetryResult<ArticleVO> getArticleByIdForUser(Integer id);

    PoetryResult<Map<Integer, List<ArticleVO>>> listSortArticle();

    /**
     * 生成文章摘要
     * @param content 文章内容
     * @param maxLength 最大长度
     * @return 摘要结果
     */
    PoetryResult<String> generateSummary(String content, Integer maxLength);

    /**
     * 获取热门文章列表（智能热度算法排序）
     * 综合考虑浏览量、点赞数、评论数、发布时间、互动率等多个因素
     * @return 热门文章列表
     */
    PoetryResult<List<ArticleVO>> getArticlesByLikesTop();

    /**
     * 异步保存文章（快速响应版本）
     * @param articleVO 文章信息
     * @return 任务ID
     */
    PoetryResult<String> saveArticleAsync(ArticleVO articleVO);

    /**
     * 异步保存文章（快速响应版本，支持翻译参数）
     * @param articleVO 文章信息
     * @param skipAiTranslation 是否跳过AI翻译
     * @param pendingTranslation 暂存的翻译数据
     * @return 任务ID
     */
    PoetryResult<String> saveArticleAsync(ArticleVO articleVO, boolean skipAiTranslation, Map<String, String> pendingTranslation);

    /**
     * 异步更新文章（快速响应版本）
     * @param articleVO 文章信息
     * @return 任务ID
     */
    PoetryResult<String> updateArticleAsync(ArticleVO articleVO);

    /**
     * 异步更新文章（快速响应版本，支持翻译参数）
     * @param articleVO 文章信息
     * @param skipAiTranslation 是否跳过AI翻译
     * @param pendingTranslation 暂存的翻译数据
     * @return 任务ID
     */
    PoetryResult<String> updateArticleAsync(ArticleVO articleVO, boolean skipAiTranslation, Map<String, String> pendingTranslation);

    /**
     * 查询文章保存状态
     * @param taskId 任务ID
     * @return 保存状态
     */
    PoetryResult<ArticleSaveStatus> getArticleSaveStatus(String taskId);

    /**
     * 流式订阅文章保存任务状态
     * @param taskId 任务ID
     * @return SSE 发射器
     */
    SseEmitter streamArticleSaveStatus(String taskId);

    /**
     * 批量流式订阅文章保存任务状态
     * @param taskIds 任务ID列表
     * @return SSE 发射器
     */
    SseEmitter streamArticleSaveStatusBatch(List<String> taskIds);

    /**
     * 获取翻译匹配的内容
     * @param id 文章ID
     * @param searchKey 搜索关键词
     * @param language 翻译语言
     * @return 翻译匹配的内容
     */
    ArticleVO getTranslationContent(Integer id, String searchKey, String language);

}
