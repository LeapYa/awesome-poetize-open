package com.ld.poetry.service;

import java.util.Map;

/**
 * 搜索引擎推送服务接口
 * 整合当前仍受支持的搜索引擎推送功能，支持百度、Bing(IndexNow)、Yandex、搜狗、神马等
 * 
 * @author LeapYa
 * @since 2025-09-22
 */
public interface SearchEnginePushService {

    /**
     * 推送URL到所有启用的搜索引擎
     * 
     * @param url 要推送的URL
     * @return 推送结果
     */
    Map<String, Object> pushUrlToAllEngines(String url);

    /**
     * 推送URL到指定搜索引擎
     * 
     * @param url 要推送的URL
     * @param engine 搜索引擎名称
     * @return 推送结果
     */
    Map<String, Object> pushUrlToEngine(String url, String engine);

    /**
     * 推送sitemap到所有启用的搜索引擎
     * 
     * @return 推送结果
     */
    Map<String, Object> pushSitemapToAllEngines();

    /**
     * 获取SEO配置（从数据库读取，带Redis缓存）
     * 
     * @return SEO配置
     */
    Map<String, Object> getSeoConfig();

    /**
     * 检查搜索引擎推送是否整体启用
     * 
     * @return 是否启用
     */
    boolean isPushEnabled();

    /**
     * 获取支持的搜索引擎列表
     * 
     * @return 搜索引擎列表
     */
    String[] getSupportedEngines();

    /**
     * 清除SEO配置的Redis缓存
     * 在SEO配置更新时调用，下次查询将从数据库重新加载
     */
    void clearSeoConfigCache();
}
