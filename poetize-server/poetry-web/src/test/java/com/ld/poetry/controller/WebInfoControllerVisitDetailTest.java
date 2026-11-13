package com.ld.poetry.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 访问榜单下钻明细的分页与各维度匹配规则单元测试。
 */
class WebInfoControllerVisitDetailTest {

    @Test
    void buildVisitDetailResultPaginatesSortsAndClampsPage() throws Exception {
        WebInfoController controller = new WebInfoController();
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("ip", "10.0.0." + i);
            item.put("num", (long) i);
            item.put("last_time", "");
            items.add(item);
        }

        Map<String, Object> page1 = buildVisitDetailResult(controller, items, 1);
        assertEquals(45, page1.get("total_sources"));
        assertEquals(1035L, page1.get("total_visits"));
        assertEquals(1, page1.get("page"));
        List<Map<String, Object>> page1Items = itemsOf(page1);
        assertEquals(20, page1Items.size());
        assertEquals(45L, page1Items.get(0).get("num"));

        Map<String, Object> page2 = buildVisitDetailResult(controller, items, 2);
        assertEquals(2, page2.get("page"));
        assertEquals(20, itemsOf(page2).size());

        Map<String, Object> page99 = buildVisitDetailResult(controller, items, 99);
        assertEquals(3, page99.get("page"));
        assertEquals(5, itemsOf(page99).size());
    }

    @Test
    void matchesSearchEngineRecordByVerificationGroup() throws Exception {
        WebInfoController controller = new WebInfoController();
        Map<String, Object> record = new HashMap<>();
        record.put("uaType", "search_engine");
        record.put("uaName", "Googlebot");
        record.put("botVerifyStatus", "verified");

        assertTrue(matches(controller, record, "ua", "Googlebot", "search_engine", "verified", null));
        assertFalse(matches(controller, record, "ua", "Googlebot", "search_engine", "unknown", null));
        assertFalse(matches(controller, record, "ua", "Bingbot", "search_engine", "verified", null));
    }

    @Test
    void matchesSpoofedSearchEngineRecord() throws Exception {
        WebInfoController controller = new WebInfoController();
        Map<String, Object> record = new HashMap<>();
        record.put("uaType", "search_engine");
        record.put("uaName", "Googlebot");
        record.put("botVerifyStatus", "failed");

        assertTrue(matches(controller, record, "ua",
                "疑似伪装 Googlebot", "spoofed_search_engine", "unknown", null));
        assertFalse(matches(controller, record, "ua",
                "Googlebot", "search_engine", "verified", null));
    }

    @Test
    void matchesArticleRecordIgnoringQueryAndLanguagePrefix() throws Exception {
        WebInfoController controller = new WebInfoController();

        assertTrue(matches(controller, recordWithPageUri("/article/12?from=home"),
                "article", "12", null, null, "example.com"));
        assertTrue(matches(controller, recordWithPageUri("/article/zh/12"),
                "article", "12", null, null, "example.com"));
        assertTrue(matches(controller, recordWithPageUri("/article/12/"),
                "article", "12", null, null, "example.com"));
        assertFalse(matches(controller, recordWithPageUri("/article/13?from=home"),
                "article", "12", null, null, "example.com"));
    }

    @Test
    void matchesReferrerRecordTreatsSiteHostAsDirect() throws Exception {
        WebInfoController controller = new WebInfoController();
        Map<String, Object> record = new HashMap<>();
        record.put("referer", "https://www.example.com/post/1");

        assertTrue(matches(controller, record, "referrer", "Direct", null, null, "www.example.com"));
        assertFalse(matches(controller, record, "referrer", "other.com", null, null, "other.com"));
    }

    @Test
    void matchesIpUserAndProvinceRecords() throws Exception {
        WebInfoController controller = new WebInfoController();

        Map<String, Object> ipRecord = new HashMap<>();
        ipRecord.put("ip", "1.2.3.4");
        assertTrue(matches(controller, ipRecord, "ip", "1.2.3.4", null, null, null));

        Map<String, Object> userRecord = new HashMap<>();
        userRecord.put("userId", 7);
        assertTrue(matches(controller, userRecord, "user", "7", null, null, null));

        Map<String, Object> provinceRecord = new HashMap<>();
        provinceRecord.put("nation", "美国");
        provinceRecord.put("province", "California");
        provinceRecord.put("city", "Los Angeles");
        assertTrue(matches(controller, provinceRecord, "province", "美国", null, null, null));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildVisitDetailResult(WebInfoController controller,
                                                       List<Map<String, Object>> items,
                                                       int page) throws Exception {
        Method method = WebInfoController.class.getDeclaredMethod(
                "buildVisitDetailResult", List.class, int.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(controller, items, page);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itemsOf(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("items");
    }

    private boolean matches(WebInfoController controller, Map<String, Object> record,
                            String dimension, String value, String uaType,
                            String statusGroup, String siteHost) throws Exception {
        Method method = WebInfoController.class.getDeclaredMethod(
                "matchesVisitRecord", Map.class, String.class, String.class,
                String.class, String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(controller, record, dimension, value, uaType, statusGroup, siteHost);
    }

    private Map<String, Object> recordWithPageUri(String pageUri) {
        Map<String, Object> record = new HashMap<>();
        record.put("pageUri", pageUri);
        return record;
    }
}
