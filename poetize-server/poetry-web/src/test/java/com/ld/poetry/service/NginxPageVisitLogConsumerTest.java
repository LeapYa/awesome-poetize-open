package com.ld.poetry.service;

import com.ld.poetry.utils.CommonQuery;
import com.ld.poetry.utils.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Nginx 页面访问日志补录的状态码边界测试。
 *
 * <p>301 只是 HTTP→HTTPS 跳转，不是页面浏览；扫描器制造大量 301 时，
 * 若补录端接纳 3xx 会直接污染访问统计。
 */
class NginxPageVisitLogConsumerTest {

    @TempDir
    Path tempDir;

    private NginxPageVisitLogConsumer consumer;
    private CommonQuery commonQuery;

    @BeforeEach
    void setUp() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.get(anyString())).thenReturn(null);
        commonQuery = mock(CommonQuery.class);

        consumer = new NginxPageVisitLogConsumer();
        ReflectionTestUtils.setField(consumer, "redisUtil", redisUtil);
        ReflectionTestUtils.setField(consumer, "commonQuery", commonQuery);
        ReflectionTestUtils.setField(consumer, "enabled", true);
        ReflectionTestUtils.setField(consumer, "maxLinesPerRun", 100);
    }

    @Test
    @DisplayName("301 跳转不补录，只有 200 页面访问进入统计")
    void only2xxIsCountedAsPageVisit() throws Exception {
        Path log = tempDir.resolve("page_visit.log");
        Files.writeString(log, String.join("\n",
                logLine("/%2eenv", 301),
                logLine("/article/hello-world", 200),
                logLine("/some-page", 302),
                logLine("/article/missing", 404)), StandardCharsets.UTF_8);
        ReflectionTestUtils.setField(consumer, "logPath", log.toString());

        consumer.consumePageVisitLog();

        verify(commonQuery, times(1)).saveHistory(
                eq("8.8.4.4"),
                eq("/article/hello-world"),
                any(), any(), any(), any());
        verify(commonQuery, never()).saveHistory(
                eq("8.8.4.4"),
                eq("/%2eenv"),
                any(), any(), any(), any());
    }

    private String logLine(String uri, int status) {
        return "{\"time\":\"2026-11-09T17:00:00+08:00\",\"ip\":\"8.8.4.4\","
                + "\"xff\":\"8.8.4.4\",\"method\":\"GET\",\"uri\":\"" + uri + "\","
                + "\"status\":" + status + ",\"referer\":\"-\","
                + "\"user_agent\":\"Mozilla/5.0\",\"accept_language\":\"zh-CN\"}";
    }
}
