package com.ld.poetry.service.impl;

import com.ld.poetry.service.MailTemplateService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮件模板服务实现
 * 使用Freemarker生成邮件内容
 *
 * @author LeapYa
 * @since 2026-01-17
 */
@Service
@Slf4j
public class MailTemplateServiceImpl implements MailTemplateService {

    @Autowired
    private Configuration freemarkerConfig;

    @Override
    public String generateSeoNotificationEmail(String title, String url, boolean success, String timestamp, Map<String, Object> results) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("title", title);
            model.put("url", url);
            model.put("success", success);
            model.put("timestamp", timestamp);
            model.put("results", results);
            
            // 辅助方法：获取搜索引擎名称
            model.put("getSearchEngineName", new SearchEngineNameMethod());

            Template template = freemarkerConfig.getTemplate("seo-notification.ftl");
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (Exception e) {
            log.error("生成SEO通知邮件模板失败", e);
            // 降级处理：返回简单的文本内容
            return "SEO推送通知生成失败: " + e.getMessage();
        }
    }
    
    /**
     * Freemarker自定义方法：转换搜索引擎名称
     */
    public static class SearchEngineNameMethod implements freemarker.template.TemplateMethodModelEx {
        @Override
        public Object exec(java.util.List arguments) {
            if (arguments.isEmpty()) {
                return "未知";
            }
            String engine = arguments.get(0).toString().toLowerCase();
            switch (engine) {
                case "baidu": return "百度搜索";
                case "google": return "谷歌搜索";
                case "bing": return "必应搜索";
                case "yandex": return "Yandex搜索";
                case "sogou": return "搜狗搜索";
                case "so": return "360搜索";
                case "shenma": return "神马搜索";
                case "yahoo": return "雅虎搜索";
                default: return engine;
            }
        }
    }
}
