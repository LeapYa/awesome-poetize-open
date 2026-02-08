package com.ld.poetry.config;

import com.alibaba.fastjson.JSON;
import com.ld.poetry.enums.CodeMsg;
import com.ld.poetry.utils.CommonQuery;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.utils.storage.FileFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class PoetryFilter extends OncePerRequestFilter {

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private FileFilter fileFilter;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        if (!"OPTIONS".equals(httpServletRequest.getMethod())) {
            try {
                // 只记录真正的页面访问，不记录API和静态资源请求
                if (isPageVisit(httpServletRequest)) {
                    commonQuery.saveHistory(PoetryUtil.getIpAddr(httpServletRequest));
                }
            } catch (Exception e) {
                // 静默处理异常，不影响正常请求流程
            }

            if (fileFilter.doFilterFile(httpServletRequest, httpServletResponse)) {
                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
                httpServletResponse.setContentType("application/json;charset=UTF-8");
                httpServletResponse.getWriter().write(JSON.toJSONString(com.ld.poetry.config.PoetryResult.fail(CodeMsg.PARAMETER_ERROR.getCode(), CodeMsg.PARAMETER_ERROR.getMsg())));
                return;
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
    
    /**
     * 判断是否为页面访问（而非API或静态资源请求）
     * @param request HTTP请求
     * @return true表示是页面访问，需要记录访问历史
     */
    private boolean isPageVisit(HttpServletRequest request) {
        // 只统计“文档导航”请求，避免把后台管理/API的 JSON 请求也算作访问量。
        // 这样站长在后台维护（一次操作会触发多次 API 请求）不会把访问量刷爆。
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String requestURI = request.getRequestURI();
        if (requestURI == null) {
            return false;
        }

        // 排除后台管理（前端管理台的 document 请求也不计入站点访问量）
        if (requestURI.startsWith("/admin")) {
            return false;
        }

        // 排除明确的API前缀
        if (requestURI.startsWith("/api/")) {
            return false;
        }

        // 排除静态资源、上传下载等
        if (isStaticOrAssetRequest(requestURI)) {
            return false;
        }

        // 排除其他服务
        if (requestURI.startsWith("/seo/") || requestURI.startsWith("/translation/")) {
            return false;
        }

        // 仅把浏览器“导航到页面”的请求计为访问
        return isDocumentNavigationRequest(request);
    }

    /**
     * 是否为浏览器导航(document/navigate)请求
     * API/Ajax 请求通常为 Sec-Fetch-Mode=cors / Sec-Fetch-Dest=empty，且 Accept 不包含 text/html。
     */
    private boolean isDocumentNavigationRequest(HttpServletRequest request) {
        String secFetchDest = request.getHeader("Sec-Fetch-Dest");
        if (secFetchDest != null && "document".equalsIgnoreCase(secFetchDest)) {
            return true;
        }

        String secFetchMode = request.getHeader("Sec-Fetch-Mode");
        if (secFetchMode != null && "navigate".equalsIgnoreCase(secFetchMode)) {
            return true;
        }

        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    /**
     * 过滤静态资源、上传下载等非页面资源
     */
    private boolean isStaticOrAssetRequest(String requestURI) {
        if (requestURI.startsWith("/static/") ||
            requestURI.startsWith("/css/") ||
            requestURI.startsWith("/js/") ||
            requestURI.startsWith("/images/") ||
            requestURI.startsWith("/favicon.ico")) {
            return true;
        }

        if (requestURI.contains("/upload/") ||
            requestURI.contains("/download/")) {
            return true;
        }

        String lower = requestURI.toLowerCase();
        return lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".gif") ||
            lower.endsWith(".ico") ||
            lower.endsWith(".css") ||
            lower.endsWith(".js") ||
            lower.endsWith(".map") ||
            lower.endsWith(".svg") ||
            lower.endsWith(".webp") ||
            lower.endsWith(".woff") ||
            lower.endsWith(".woff2") ||
            lower.endsWith(".ttf") ||
            lower.endsWith(".eot");
    }
}
