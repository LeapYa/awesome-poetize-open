package com.ld.poetry.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XSS过滤工具类
 * 提供HTML转义和输入验证功能，防止XSS攻击
 * 使用OWASP Java HTML Sanitizer实现
 * 
 * @author LeapYa
 * @since 2025-11-08
 */
@Slf4j
@Component
public class XssFilterUtil {

    private static final Pattern NUMERIC_ENTITY_PATTERN = Pattern.compile("&#(x[0-9a-fA-F]+|X[0-9a-fA-F]+|\\d+);");

    private static final Pattern TEXT_ENTITY_PATTERN =
            Pattern.compile("&(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);", Pattern.CASE_INSENSITIVE);

    // 严格模式：不允许任何HTML标签，只允许纯文本
    private static final PolicyFactory STRICT_POLICY = new HtmlPolicyBuilder()
            .toFactory();

    // 基本格式模式：允许基本文本格式标签
    private static final PolicyFactory BASIC_FORMAT_POLICY = new HtmlPolicyBuilder()
            .allowElements("b", "i", "u", "em", "strong", "p", "br", "span")
            .allowAttributes("class").onElements("span", "p")
            .toFactory();

    // 富文本模式：允许更多HTML标签，但仍保持安全性
    private static final PolicyFactory RICH_TEXT_POLICY = new HtmlPolicyBuilder()
            .allowElements(
                    "a", "b", "i", "u", "em", "strong", "p", "br", "span", "div",
                    "ul", "ol", "li", "blockquote", "code", "pre", "h1", "h2", "h3", "h4", "h5", "h6")
            .allowAttributes("href").onElements("a")
            .allowAttributes("class", "style").onElements("span", "p", "div")
            .allowAttributes("class").onElements("ul", "ol", "li", "blockquote", "code", "pre")
            .allowStandardUrlProtocols()
            .toFactory();

    /**
     * 清理输入内容，移除潜在的XSS攻击代码
     * 使用严格模式，不允许任何HTML标签
     * 
     * @param content 原始内容
     * @return 清理后的安全内容
     */
    public static String clean(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }

        try {
            String sanitized = STRICT_POLICY.sanitize(content);
            return restoreSafeCharacterReferences(sanitized);
        } catch (Exception e) {
            log.error("XSS过滤处理异常: {}", e.getMessage(), e);
            // 出现异常时，返回转义后的内容
            return escapeHtml(content);
        }
    }

    /**
     * 还原 {@link #clean(String)} 留在纯文本字段里的 HTML 实体，供渲染前解码一次，
     * 否则渲染方再转义会让页面露出 {@code &#39;} 字面量。单趟替换，{@code &amp;lt;} 不会解成 {@code <}。
     * <p>只能用于确定经过 clean 的字段；裸文本调用会改写作者原意。
     */
    public static String decodeTextEntities(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }

        Matcher matcher = TEXT_ENTITY_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String body = matcher.group(1);
            String replacement = switch (body.toLowerCase()) {
                case "amp" -> "&";
                case "lt" -> "<";
                case "gt" -> ">";
                case "quot" -> "\"";
                case "apos" -> "'";
                default -> decodeNumericEntity(body, matcher.group(0));
            };
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * HTML转义，将特殊字符转换为HTML实体
     * 
     * @param content 原始内容
     * @return 转义后的内容
     */
    public static String escapeHtml(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }

        return content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * 验证输入内容是否包含潜在的XSS攻击代码
     * 
     * @param content 待验证的内容
     * @return 如果包含XSS攻击代码返回true，否则返回false
     */
    public static boolean containsXss(String content) {
        if (StringUtils.isBlank(content)) {
            return false;
        }

        // 使用严格模式清理后，如果内容发生变化，说明包含XSS风险
        String cleaned = STRICT_POLICY.sanitize(content);
        return !content.equals(cleaned);
    }

    /**
     * 验证并清理输入内容，适用于评论、文章等用户输入
     * 
     * @param content   原始内容
     * @param maxLength 允许的最大长度
     * @return 验证和清理后的内容，如果输入无效返回null
     */
    public static String validateAndClean(String content, int maxLength) {
        // 检查空值
        if (content == null) {
            return null;
        }

        // 去除前后空格
        content = content.trim();

        // 检查长度
        if (content.length() > maxLength) {
            log.warn("输入内容超过最大长度限制: {}, 实际长度: {}", maxLength, content.length());
            return null;
        }

        // 检查是否为空
        if (content.isEmpty()) {
            return null;
        }

        // 清理XSS风险
        String cleaned = clean(content);

        // 验证清理后是否为空
        if (cleaned.isEmpty()) {
            return null;
        }

        return cleaned;
    }

    /**
     * 允许部分HTML标签的内容清理（适用于富文本编辑器内容）
     * 
     * @param content 原始内容
     * @return 清理后的内容
     */
    public static String cleanWithAllowedTags(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }

        try {
            String sanitized = RICH_TEXT_POLICY.sanitize(content);
            return restoreSafeCharacterReferences(sanitized);
        } catch (Exception e) {
            log.error("XSS过滤处理异常: {}", e.getMessage(), e);
            // 出现异常时，返回转义后的内容
            return escapeHtml(content);
        }
    }

    /**
     * 允许基本格式标签的内容清理（适用于需要基本格式的场景）
     * 
     * @param content 原始内容
     * @return 清理后的内容
     */
    public static String cleanWithBasicFormat(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }

        try {
            String sanitized = BASIC_FORMAT_POLICY.sanitize(content);
            return restoreSafeCharacterReferences(sanitized);
        } catch (Exception e) {
            log.error("XSS过滤处理异常: {}", e.getMessage(), e);
            // 出现异常时，返回转义后的内容
            return escapeHtml(content);
        }
    }

    private static String restoreSafeCharacterReferences(String content) {
        Matcher matcher = NUMERIC_ENTITY_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String entityBody = matcher.group(1);
            String replacement = matcher.group(0);
            try {
                int codePoint = entityBody.charAt(0) == 'x' || entityBody.charAt(0) == 'X'
                        ? Integer.parseInt(entityBody.substring(1), 16)
                        : Integer.parseInt(entityBody);
                if (isSafeTextCodePoint(codePoint)) {
                    replacement = new String(Character.toChars(codePoint));
                }
            } catch (IllegalArgumentException ignored) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static boolean isSafeTextCodePoint(int codePoint) {
        return Character.isValidCodePoint(codePoint)
                && !Character.isISOControl(codePoint)
                && codePoint != '<'
                && codePoint != '>'
                && codePoint != '&'
                && codePoint != '"'
                && codePoint != '\'';
    }

    private static String decodeNumericEntity(String body, String fallback) {
        try {
            int codePoint = body.charAt(1) == 'x' || body.charAt(1) == 'X'
                    ? Integer.parseInt(body.substring(2), 16)
                    : Integer.parseInt(body.substring(1));
            return Character.isValidCodePoint(codePoint) ? new String(Character.toChars(codePoint)) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
