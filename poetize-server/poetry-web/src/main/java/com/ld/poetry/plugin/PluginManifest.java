package com.ld.poetry.plugin;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 插件清单（对应 manifest.json）
 *
 * @author LeapYa
 * @since 2026-02-19
 */
@Data
public class PluginManifest {

    /** 插件唯一标识（小写字母+连字符，如 word-count） */
    private String name;

    /** 插件版本号（如 1.0.0） */
    private String version;

    /** 插件显示名称 */
    private String displayName;

    /** 插件描述 */
    private String description;

    /** 作者 */
    private String author;

    /**
     * 插件类型，对应 SysPlugin.TYPE_* 常量
     * 内置类型：mouse_click_effect, particle_effect, waifu_model, editor, article_theme, payment, ai_tool
     * 自定义类型：article_widget, sidebar_widget, comment_widget 等
     */
    private String pluginType;

    /** 最低兼容的应用版本 */
    private String minAppVersion;

    /** 插件所需权限列表 */
    private List<String> permissions;

    /** 钩子注册 */
    private HooksConfig hooks;

    /** 配置项 Schema（用于自动生成配置表单） */
    private Map<String, ConfigField> configSchema;

    /** AI 工具用途说明 */
    private String useCase;

    /** 希望从接口结果中提炼出的输出 */
    private String expectedOutput;

    /** 典型用户提问示例 */
    private List<String> exampleQuestions;

    /** AI 工具定义（用于 Tool Calling） */
    private ToolConfig tool;

    /** AI 工具运行时配置（当前支持 http） */
    private HttpRuntimeConfig runtime;

    /** 是否包含数据库 SQL */
    private boolean hasSql;

    /** 是否包含后端 Groovy 脚本 */
    private boolean hasBackend;

    /** 是否包含前端 JS */
    private boolean hasFrontend;

    @Data
    public static class HooksConfig {
        /** 前端钩子点列表 */
        private List<String> frontend;
        /** 后端钩子点列表 */
        private List<String> backend;
    }

    @Data
    public static class ConfigField {
        /** 字段类型：string, number, boolean, select */
        private String type;
        /** 默认值 */
        private Object defaultValue;
        /** 标签（显示名称） */
        private String label;
        /** 描述 */
        private String description;
        /** select 类型的选项 */
        private List<Map<String, String>> options;
    }

    @Data
    public static class ToolConfig {
        /** 工具名称（模型可调用的函数名） */
        private String name;
        /** 工具描述 */
        private String description;
        /** JSON Schema 输入定义 */
        private Map<String, Object> inputSchema;
    }

    @Data
    public static class HttpRuntimeConfig {
        /** 运行时类型，当前支持 http */
        private String type;
        /** HTTP 方法，如 GET/POST */
        private String method;
        /** 请求地址 */
        private String url;
        /** 请求头模板，支持 {{args.xxx}} / {{config.xxx}} */
        private Map<String, Object> headers;
        /** Query 参数模板，支持 {{args.xxx}} / {{config.xxx}} */
        private Map<String, Object> query;
        /** Body 模板，支持 {{args.xxx}} / {{config.xxx}} */
        private Object body;
        /** 响应提取路径，如 data.items 或 organic_results */
        private String responsePath;
        /** 请求超时（毫秒） */
        private Integer timeoutMs;
        /** Content-Type，默认 application/json */
        private String contentType;
    }
}
