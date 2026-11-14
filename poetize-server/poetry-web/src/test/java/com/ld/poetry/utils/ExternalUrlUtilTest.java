package com.ld.poetry.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExternalUrlUtilTest {

    @Test
    void normalizeShouldRejectPseudoProtocols() {
        // 友链由访客提交，这两个协议落到 <a href> 上会在站点源里执行脚本
        assertNull(ExternalUrlUtil.normalizeHttpUrl("javascript:alert(document.cookie)"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("JaVaScRiPt:alert(1)"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("data:text/html,<script>alert(1)</script>"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("vbscript:msgbox(1)"));
    }

    @Test
    void normalizeShouldDefeatWhitespaceObfuscation() {
        // 浏览器解析 URL 时会忽略这些字符，不清理就会绕过协议判定
        assertNull(ExternalUrlUtil.normalizeHttpUrl("java\tscript:alert(1)"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("java\nscript:alert(1)"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("  javascript:alert(1)  "));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("java\u00a0script:alert(1)"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("java\u3000script:alert(1)"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("\u0001javascript:alert(1)"));
    }

    @Test
    void normalizeShouldPrependSchemeWhenMissing() {
        assertEquals("https://example.com", ExternalUrlUtil.normalizeHttpUrl("example.com"));
        assertEquals("https://example.com/path?a=1", ExternalUrlUtil.normalizeHttpUrl("example.com/path?a=1"));
        assertEquals("https://other.com", ExternalUrlUtil.normalizeHttpUrl("//other.com"));
    }

    @Test
    void normalizeShouldKeepValidAbsoluteUrlIntact() {
        assertEquals("https://example.com/a", ExternalUrlUtil.normalizeHttpUrl("https://example.com/a"));
        assertEquals("http://example.com/a", ExternalUrlUtil.normalizeHttpUrl("http://example.com/a"));
        assertEquals("HTTPS://Example.com", ExternalUrlUtil.normalizeHttpUrl("HTTPS://Example.com"));
        assertEquals("https://example.com/a?b=1&c=2", ExternalUrlUtil.normalizeHttpUrl("https://example.com/a?b=1&c=2"));
    }

    @Test
    void normalizeShouldRejectBlankOrHostlessInput() {
        assertNull(ExternalUrlUtil.normalizeHttpUrl(null));
        assertNull(ExternalUrlUtil.normalizeHttpUrl(""));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("   "));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("https://"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("/article/1"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("https://?x=1"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("https://#frag"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("https:///evil.com"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("https:\\\\evil.com"));
    }

    @Test
    void normalizeShouldEncodeInteriorWhitespaceRatherThanDropIt() {
        // 删掉空格会静默改写链接目标，必须编码保留
        assertEquals("https://example.com/my%20page", ExternalUrlUtil.normalizeHttpUrl("https://example.com/my page"));
        assertEquals("https://example.com/a?q=hello%20world", ExternalUrlUtil.normalizeHttpUrl("https://example.com/a?q=hello world"));
        assertEquals("https://example.com/a%20b", ExternalUrlUtil.normalizeHttpUrl("example.com/a b"));
    }

    @Test
    void normalizeShouldTrimEdgeWhitespaceAndCleanScheme() {
        assertEquals("https://example.com", ExternalUrlUtil.normalizeHttpUrl("  https://example.com  "));
        assertEquals("https://example.com", ExternalUrlUtil.normalizeHttpUrl("\u00a0https://example.com\u00a0"));
        // scheme 里夹空白时主机仍取紧凑形态，不会拼出 https:/%20/ 这种坏地址
        assertEquals("https://example.com", ExternalUrlUtil.normalizeHttpUrl("https ://example.com"));
        assertEquals("https://other.com", ExternalUrlUtil.normalizeHttpUrl("/ /other.com"));
    }
}