package com.ld.poetry.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ld.poetry.entity.Label;
import com.ld.poetry.entity.Sort;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class ArticleVO {

    private Integer id;

    private Integer userId;

    //查询为空时，随机选择
    private String articleCover;

    @NotBlank(message = "文章标题不能为空")
    private String articleTitle;

    @NotBlank(message = "文章内容不能为空")
    private String articleContent;

    private Integer viewCount;

    private Boolean commentStatus;

    private Boolean recommendStatus;

    private String videoUrl;

    private String password;

    private String tips;

    private Boolean viewStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private String updateBy;

    // sortId 和 labelId 允许为空：当传入 sortName/labelName 时由后端自动解析创建
    private Integer sortId;

    private Integer labelId;

    // 需要查询封装
    private Integer commentCount;
    private String username;
    private Sort sort;
    private Label label;
    private Boolean hasVideo = false;
    
    // API接口兼容字段
    private String title;      // 对应 articleTitle
    private String content;    // 对应 articleContent
    private Integer classify;  // 对应 sortId
    private String cover;      // 对应 articleCover
    private String summary;    // 对应 tips
    
    // 搜索引擎推送控制
    private Boolean submitToSearchEngine; // 是否推送至搜索引擎
    
    // 翻译搜索相关字段
    private String matchedLanguage;     // 匹配的语言（zh/en等）

    private Boolean isTranslationMatch; // 是否匹配的翻译内容
    private Boolean hasTranslationMatch; // 是否同时有翻译匹配（当原文和翻译都匹配时）
    
    // 分类名称和标签名称（用于API创建文章时自动创建分类和标签）
    private String sortName;    // 分类名称
    private String labelName;   // 标签名称
    
    // 文章访问链接（动态生成，用于API和MCP工具）
    private String articleUrl;
    
    // 翻译内容（按需返回，优化性能）
    private String translatedTitle;    // 翻译后的标题
    private String translatedContent;  // 翻译后的内容

    // 文章主题配置（合并到文章接口，避免额外请求导致样式闪烁）
    private String articleThemeConfig; // JSON 字符串，来自激活的 article_theme 插件的 pluginConfig
}
