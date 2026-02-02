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
}
