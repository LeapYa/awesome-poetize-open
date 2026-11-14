package com.ld.poetry.utils;

import java.util.regex.Pattern;

/**
 * 站外链接归一化工具。
 * <p>友链、收藏夹等由访客提交的地址最终会渲染成 {@code <a href>}，
 * 仅靠 {@link XssFilterUtil#clean(String)} 剥标签无法拦截 {@code javascript:}、
 * {@code data:} 这类伪协议（它们在浏览器里会以站点源执行）。这里统一做两件事：
 * <ol>
 *     <li>缺少 scheme 的地址补 {@code https://}（避免 {@code example.com} 被当成站内相对路径）；</li>
 *     <li>只放行 http/https，其余协议一律判定为非法并返回 {@code null}，由调用方决定拒绝或降级为纯文本。</li>
 * </ol>
 * 校验前会先剔除空白与控制字符，防止 {@code java\tscript:} 这类拼接绕过。
 *
 * @author LeapYa
 */
public final class ExternalUrlUtil {

    /** 形如 scheme:// 的前缀，用于判断地址是否已带协议 */
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");

    /** 唯一放行的协议前缀 */
    private static final Pattern ALLOWED_PREFIX = Pattern.compile("^(https?)://", Pattern.CASE_INSENSITIVE);

    /** 空白与控制字符：浏览器解析 URL 时会忽略它们，必须先剔除再判定协议 */
    private static final Pattern BLANK_AND_CONTROL = Pattern.compile("[\\u0000-\\u0020\\u007F\\u00A0]");

    private ExternalUrlUtil() {
    }

    /**
     * 把地址归一化为 http/https 绝对地址。
     *
     * @param raw 原始地址，可为空
     * @return 归一化后的地址；为空、非 http/https、或无主机部分时返回 {@code null}
     */
    public static String normalizeHttpUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String value = BLANK_AND_CONTROL.matcher(raw).replaceAll("");
        if (value.isEmpty()) {
            return null;
        }
        if (value.startsWith("//")) {
            // 协议相对写法，按 https 补齐
            value = "https:" + value;
        } else if (!SCHEME_PATTERN.matcher(value).matches()) {
            value = "https://" + value;
        }
        if (!ALLOWED_PREFIX.matcher(value).lookingAt()) {
            return null;
        }
        String afterScheme = value.substring(value.indexOf("://") + 3);
        if (afterScheme.isEmpty() || afterScheme.startsWith("/")) {
            return null;
        }
        return value;
    }

    /**
     * 地址是否为可安全渲染的 http/https 绝对地址。
     */
    public static boolean isSafeLink(String raw) {
        return normalizeHttpUrl(raw) != null;
    }
}
