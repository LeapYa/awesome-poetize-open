package com.ld.poetry.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class XssFilterUtilTest {

    @Test
    public void testCleanWithChineseCharacters() {
        String input = "你好，世界。这是网站简介！「这里有引号」，（还有括号），以及：冒号、分号；";
        String expected = "你好，世界。这是网站简介！「这里有引号」，（还有括号），以及：冒号、分号；";
        String actual = XssFilterUtil.clean(input);
        assertEquals(expected, actual, "All Chinese punctuation marks should be preserved.");
    }

    @Test
    public void testCleanWithXss() {
        String input = "你好<script>alert('xss')</script>，世界";
        String expected = "你好，世界";
        String actual = XssFilterUtil.clean(input);
        assertEquals(expected, actual, "Script tags should still be removed.");
    }

    @Test
    public void testCleanWithHtmlEntities() {
        String input = "<b>粗体</b><i>斜体</i>";
        // STRICT_POLICY (clean method) should remove all tags
        String expected = "粗体斜体";
        String actual = XssFilterUtil.clean(input);
        assertEquals(expected, actual, "STRICT_POLICY should remove all HTML tags.");
    }

    @Test
    public void testCleanWithBasicFormat() {
        String input = "<b>粗体</b><i>斜体</i>";
        String expected = "<b>粗体</b><i>斜体</i>";
        String actual = XssFilterUtil.cleanWithBasicFormat(input);
        assertEquals(expected, actual, "BASIC_FORMAT_POLICY should allow basic tags.");
    }

    @Test
    public void testCleanDoesNotReviveEncodedHtmlTags() {
        String input = "&lt;img src=x onerror=alert(1)&gt;";

        String actual = XssFilterUtil.clean(input);

        assertFalse(actual.contains("<img"), "Encoded tags must not be unescaped into live HTML.");
        assertTrue(actual.contains("&lt;img"), "Encoded tags should remain encoded for v-html rendering.");
    }

    @Test
    public void testBasicFormatDoesNotReviveEncodedHtmlTags() {
        String input = "&lt;img src=x onerror=alert(1)&gt;<b>ok</b>";

        String actual = XssFilterUtil.cleanWithBasicFormat(input);

        assertFalse(actual.contains("<img"), "Encoded disallowed tags must not become live HTML.");
        assertTrue(actual.contains("&lt;img"), "Encoded disallowed tags should remain encoded.");
        assertTrue(actual.contains("<b>ok</b>"), "Allowed formatting tags should still be preserved.");
    }

    @Test
    public void testDecodeTextEntitiesRestoresCleanOutput() {
        // clean 的输出会原样入库，解码必须能还原站长最初填写的纯文本
        String[] inputs = {"张三 & 李四", "A < B > C", "It's a blog", "Tom & Jerry's", "说\"你好\"的朋友"};
        for (String input : inputs) {
            assertEquals(input, XssFilterUtil.decodeTextEntities(XssFilterUtil.clean(input)),
                    "clean 后再解码应还原原文: " + input);
        }
    }

    @Test
    public void testDecodeTextEntitiesIsSinglePass() {
        assertEquals("&lt;", XssFilterUtil.decodeTextEntities("&amp;lt;"), "不得二次解码成 <");
        assertEquals("&lt;", XssFilterUtil.decodeTextEntities("&#38;lt;"), "数字实体也不得二次解码");
        assertEquals("&amp;", XssFilterUtil.decodeTextEntities("&amp;amp;"), "只解一层");
    }

    @Test
    public void testDecodeTextEntitiesHandlesNumericEntities() {
        assertEquals("\uD83D\uDE00", XssFilterUtil.decodeTextEntities("&#128512;"), "十进制 emoji");
        assertEquals("\uD83D\uDE00", XssFilterUtil.decodeTextEntities("&#x1F600;"), "十六进制 emoji");
        assertEquals("\uD83D\uDE00", XssFilterUtil.decodeTextEntities("&#X1F600;"), "大写 X 前缀");
        assertEquals("&#999999999;", XssFilterUtil.decodeTextEntities("&#999999999;"), "超出码点上限原样保留");
        assertEquals("&#xFFFFFFFF;", XssFilterUtil.decodeTextEntities("&#xFFFFFFFF;"), "十六进制越界原样保留");
    }

    @Test
    public void testDecodeTextEntitiesPassesThroughBlankAndPlain() {
        assertNull(XssFilterUtil.decodeTextEntities(null), "null 原样返回");
        assertEquals("", XssFilterUtil.decodeTextEntities(""), "空串原样返回");
        assertEquals("普通友链名称", XssFilterUtil.decodeTextEntities("普通友链名称"), "无实体不变");
        assertEquals("a/b/c 路径站", XssFilterUtil.decodeTextEntities("a/b/c 路径站"), "斜杠不被解码");
        assertEquals("&unknown;", XssFilterUtil.decodeTextEntities("&unknown;"), "未知命名实体原样保留");
    }
}
