package com.ld.poetry.controller;

import com.ld.poetry.service.SeoConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 根路径 favicon 兼容处理
 */
@RestController
@RequestMapping("/seo")
@Slf4j
public class FaviconController {

    @Autowired
    private SeoConfigService seoConfigService;

    @GetMapping("/favicon.ico")
    public void favicon(HttpServletResponse response) throws IOException {
        String siteIcon = getSiteIcon();
        if (!StringUtils.hasText(siteIcon)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String target = normalizeLocation(siteIcon);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", target);
    }

    private String getSiteIcon() {
        try {
            Map<String, Object> config = seoConfigService.getSeoConfigAsJson();
            if (config == null) {
                return null;
            }
            Object value = config.get("site_icon");
            return value != null ? value.toString().trim() : null;
        } catch (Exception e) {
            log.warn("读取site_icon失败: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeLocation(String url) {
        String target = url.trim();
        if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("/")) {
            return target;
        }
        return "/" + target;
    }
}
