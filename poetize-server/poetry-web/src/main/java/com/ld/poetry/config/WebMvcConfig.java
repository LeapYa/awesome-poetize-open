package com.ld.poetry.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private PoetryFilter poetryFilter;
    
    @Value("${local.uploadUrl:/app/static/}")
    private String uploadUrl;

    /**
     * 注册安全过滤器
     */
    @Bean
    public FilterRegistrationBean<SecurityFilter> securityFilterRegistration() {
        FilterRegistrationBean<SecurityFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(securityFilter);
        registration.addUrlPatterns("/*");
        registration.setName("securityFilter");
        registration.setOrder(1); // 设置过滤器优先级，数字越小优先级越高
        return registration;
    }

    /**
     * 注册访问量统计过滤器
     */
    @Bean
    public FilterRegistrationBean<PoetryFilter> poetryFilterRegistration() {
        FilterRegistrationBean<PoetryFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(poetryFilter);
        registration.addUrlPatterns("/*");
        registration.setName("poetryFilter");
        registration.setOrder(2); // 设置在SecurityFilter之后执行
        return registration;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 只处理用户上传的文件资源，不处理前端静态资源
        // 确保路径以 file: 开头并以 / 结尾
        String location = uploadUrl;
        if (!location.startsWith("file:")) {
            location = "file:" + location;
        }
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        
        log.info("配置静态资源映射: /static/** -> {}", location);
        registry.addResourceHandler("/static/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
} 
