package com.ld.poetry.service;

import com.ld.poetry.service.impl.SearchEnginePushServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class SearchEnginePushServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private SearchEnginePushServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new SearchEnginePushServiceImpl());
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void getSupportedEngines_returnsOnlyActivePushEngines() {
        assertArrayEquals(
                new String[]{"baidu", "bing", "yandex", "sogou", "shenma"},
                service.getSupportedEngines());
    }

    @Test
    void pushUrlToEngine_whenGoogleYahooOrSoRequested_returnsUnsupported() {
        doReturn(Map.of("enable", true)).when(service).getSeoConfig();

        assertUnsupportedEngine("google");
        assertUnsupportedEngine("yahoo");
        assertUnsupportedEngine("so");
    }

    @Test
    void pushUrlToEngine_whenBingKeyMissing_returnsFailureWithoutLegacyPingFallback() {
        doReturn(Map.of(
                "enable", true,
                "bing_push_enabled", true,
                "bing_api_key", ""
        )).when(service).getSeoConfig();

        Map<String, Object> result = service.pushUrlToEngine("https://example.com/article/1", "bing");

        assertEquals(false, result.get("success"));
        assertEquals("Bing IndexNow key未配置", result.get("message"));
        verifyNoInteractions(restTemplate);
    }

    private void assertUnsupportedEngine(String engine) {
        Map<String, Object> result = service.pushUrlToEngine("https://example.com/article/1", engine);
        assertFalse(Boolean.TRUE.equals(result.get("success")));
        assertEquals("不支持的搜索引擎: " + engine, result.get("message"));
    }
}
