package com.ld.poetry.service;

import java.util.Map;

/**
 * 邮件模板服务
 * 负责使用模板引擎生成邮件内容
 *
 * @author LeapYa
 * @since 2026-01-17
 */
public interface MailTemplateService {

    /**
     * 生成SEO推送结果通知邮件内容
     *
     * @param title 文章标题
     * @param url 文章链接
     * @param success 是否全部/部分成功
     * @param timestamp 推送时间
     * @param results 各搜索引擎的推送详情
     * @return 生成的HTML邮件内容
     */
    String generateSeoNotificationEmail(String title, String url, boolean success, String timestamp, Map<String, Object> results);
}
