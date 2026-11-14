package com.ld.poetry.utils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 站外链接归一化工具。
 * <p>友链由访客投稿、收藏夹由站长维护，两者最终都会渲染成 {@code <a href>} 或交给 {@code window.open}，
 * 仅靠 {@link XssFilterUtil#clean(String)} 剥标签无法拦截 {@code javascript:}、
 * {@code data:} 这类伪协议（它们在浏览器里会以站点源执行）。这里统一做两件事：
 * <ol>
 *     <li>缺少 scheme 的地址补 {@code https://}（避免 {@code example.com} 被当成站内相对路径）；</li>
 *     <li>只放行 http/https，其余协议一律判定为非法并返回 {@code null}，由调用方决定拒绝或降级为纯文本。</li>
 * </ol>
 * 协议判定基于剔除全部空白与控制字符的副本，防止 {@code java\tscript:} 这类拼接绕过；
 * 但路径与查询串中的空白会被百分号编码而非删除，避免静默改写链接目标。
 *
 * @author LeapYa
 */
public final class ExternalUrlUtil {

    /** 形如 scheme:// 的前缀，用于判断地址是否已带协议 */
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");

    /** 唯一放行的协议前缀 */
    private static final Pattern ALLOWED_PREFIX = Pattern.compile("^(https?)://", Pattern.CASE_INSENSITIVE);

    /** 浏览器解析 URL 时会忽略的控制字符，必须先剔除再判定协议 */
    private static final Pattern CONTROL = Pattern.compile("[\\u0000-\\u001F\\u007F]");

    /** 含 NBSP、全角空格等 Unicode 空白：String.strip() 不认 NBSP，这里显式列出 */
    private static final String WHITESPACE_CLASS =
            "\\s\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF";

    private static final Pattern WHITESPACE = Pattern.compile("[" + WHITESPACE_CLASS + "]");

    private static final Pattern EDGE_WHITESPACE =
            Pattern.compile("^[" + WHITESPACE_CLASS + "]+|[" + WHITESPACE_CLASS + "]+$");

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
        String value = EDGE_WHITESPACE.matcher(CONTROL.matcher(raw).replaceAll("")).replaceAll("");
        if (value.isEmpty()) {
            return null;
        }

        String compact = WHITESPACE.matcher(value).replaceAll("");
        String prefix;
        if (compact.startsWith("//")) {
            // 协议相对写法，按 https 补齐
            prefix = "https:";
        } else if (SCHEME_PATTERN.matcher(compact).matches()) {
            prefix = "";
        } else {
            prefix = "https://";
        }

        String probe = prefix + compact;
        if (!ALLOWED_PREFIX.matcher(probe).lookingAt()) {
            return null;
        }
        int probeAuthorityStart = probe.indexOf("://") + 3;
        int hostLength = authorityLength(probe.substring(probeAuthorityStart));
        if (hostLength == 0) {
            return null;
        }

        // scheme 与主机取判定过的紧凑形态（主机里夹空白本就非法），
        // 路径/查询/片段取原值并编码其中的空白，保留用户真实意图
        String original = prefix + value;
        int originalAuthorityStart = original.indexOf("://");
        if (originalAuthorityStart < 0) {
            return probe;
        }
        int pathStart = firstDelimiter(original, originalAuthorityStart + 3);
        String tail = pathStart < 0 ? "" : encodeWhitespace(original.substring(pathStart));
        return probe.substring(0, probeAuthorityStart + hostLength) + tail;
    }

    /** 主机部分的长度：遇到路径、查询或片段分隔符即结束 */
    private static int authorityLength(String authority) {
        int end = firstDelimiter(authority, 0);
        return end < 0 ? authority.length() : end;
    }

    private static int firstDelimiter(String value, int from) {
        for (int i = from; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                return i;
            }
        }
        return -1;
    }

    private static String encodeWhitespace(String value) {
        Matcher matcher = WHITESPACE.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(percentEncode(matcher.group())));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String percentEncode(String value) {
        StringBuilder sb = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            sb.append('%').append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
