package com.ld.poetry.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器
 * 处理前端路由请求，返回index.html
 */
@Controller
public class HomeController {

    /**
     * 处理根路径请求，返回前端页面
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * 处理前端路由请求，避免刷新404
     * 注意：排除 /api/ 开头的路径，避免拦截API请求
     */
    @GetMapping({ "/home", "/user/**", "/admin/**", "/sort/**", "/label/**", "/comment/**", "/tree/**", "/weiyan/**",
            "/music/**", "/picture/**", "/video/**", "/love/**", "/funny/**", "/favorites/**", "/im/**" })
    public String forwardToIndex() {
        return "forward:/index.html";
    }

    /**
     * 单独处理前端文章路由，排除API路径
     * 只匹配非API的文章路径，如 /article/123, /article/detail/123 等
     */
    @GetMapping("/article/{path:(?!api).*}")
    public String forwardArticleToIndex() {
        return "forward:/index.html";
    }
}