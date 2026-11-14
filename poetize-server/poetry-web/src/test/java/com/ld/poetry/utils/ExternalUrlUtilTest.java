package com.ld.poetry.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }

    @Test
    void normalizeShouldRejectBlankOrHostlessInput() {
        assertNull(ExternalUrlUtil.normalizeHttpUrl(null));
        assertNull(ExternalUrlUtil.normalizeHttpUrl(""));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("   "));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("https://"));
        assertNull(ExternalUrlUtil.normalizeHttpUrl("/article/1"));
    }

    @Test
    void isSafeLinkShouldFollowNormalization() {
        assertTrue(ExternalUrlUtil.isSafeLink("example.com"));
        assertFalse(ExternalUrlUtil.isSafeLink("javascript:alert(1)"));
    }
}
