package com.ld.poetry.service;

import org.junit.jupiter.api.Test;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IP2Region数据库文件测试
 * 验证IP2Region 3.x版本和数据库文件是否正常工作
 * 支持 IPv4 和 IPv6 双栈，自动下载数据库
 */
public class Ip2RegionTest {

    private static final String IPV4_DB_URL = "https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region_v4.xdb";
    private static final String IPV6_DB_URL = "https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region_v6.xdb";

    private Path getTestDataDir() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "ip2region_test");
    }

    private File ensureDbFile(String fileName, String downloadUrl) throws Exception {
        Path dataDir = getTestDataDir();
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }

        Path filePath = dataDir.resolve(fileName);
        File file = filePath.toFile();

        if (file.exists() && file.length() > 1024 * 1024) {
            return file;
        }

        System.out.println("下载测试数据库: " + fileName);
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("下载失败: HTTP " + conn.getResponseCode());
        }

        try (InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("下载完成: " + file.length() / 1024 / 1024 + " MB");
        return file;
    }

    @Test
    public void testIp2RegionDatabase() {
        Searcher searcher = null;
        try {
            File dbFile = ensureDbFile("ip2region_v4.xdb", IPV4_DB_URL);
            LongByteArray cBuff = Searcher.loadContentFromFile(dbFile.getAbsolutePath());
            searcher = Searcher.newWithBuffer(Version.IPv4, cBuff);

            assertNotNull(searcher, "IP2Region搜索器应该能够正常初始化");

            // 测试一些已知的IP地址
            String result1 = searcher.search("8.8.8.8");
            assertNotNull(result1, "应该能够解析公网IP");
            System.out.println("8.8.8.8 -> " + result1);

            String result2 = searcher.search("114.114.114.114");
            assertNotNull(result2, "应该能够解析114DNS的IP");
            System.out.println("114.114.114.114 -> " + result2);

            String result3 = searcher.search("61.135.185.32");
            assertNotNull(result3, "应该能够解析百度的IP");
            System.out.println("61.135.185.32 -> " + result3);

            System.out.println("IP2Region IPv4 数据库文件工作正常！");

        } catch (Exception e) {
            fail("IP2Region数据库测试失败: " + e.getMessage());
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Test
    public void testIPv6Database() {
        Searcher searcher = null;
        try {
            File dbFile = ensureDbFile("ip2region_v6.xdb", IPV6_DB_URL);
            LongByteArray cBuff = Searcher.loadContentFromFile(dbFile.getAbsolutePath());
            searcher = Searcher.newWithBuffer(Version.IPv6, cBuff);

            assertNotNull(searcher, "IP2Region IPv6 搜索器应该能够正常初始化");

            // 测试一些已知的 IPv6 地址
            String result1 = searcher.search("2001:4860:4860::8888"); // Google DNS IPv6
            assertNotNull(result1, "应该能够解析 Google IPv6 DNS");
            System.out.println("2001:4860:4860::8888 -> " + result1);

            String result2 = searcher.search("240e:3b7:3272:d8d0:db09:c067:8d59:539e"); // 中国 IPv6
            assertNotNull(result2, "应该能够解析中国 IPv6 地址");
            System.out.println("240e:3b7:3272:d8d0:db09:c067:8d59:539e -> " + result2);

            System.out.println("IP2Region IPv6 数据库文件工作正常！");

        } catch (Exception e) {
            fail("IP2Region IPv6 数据库测试失败: " + e.getMessage());
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Test
    public void testParseResult() {
        Searcher searcher = null;
        try {
            File dbFile = ensureDbFile("ip2region_v4.xdb", IPV4_DB_URL);
            LongByteArray cBuff = Searcher.loadContentFromFile(dbFile.getAbsolutePath());
            searcher = Searcher.newWithBuffer(Version.IPv4, cBuff);

            String searchResult = searcher.search("61.135.185.32");
            assertNotNull(searchResult, "搜索结果不应为空");

            // IP2Region格式: 国家|区域|省份|城市|ISP
            String[] regions = searchResult.split("\\|");
            assertTrue(regions.length >= 4, "解析结果应该包含至少4个部分");

            System.out.println("解析结果详情:");
            for (int i = 0; i < regions.length; i++) {
                String[] labels = { "国家", "区域", "省份", "城市", "ISP" };
                String label = i < labels.length ? labels[i] : "字段" + i;
                System.out.println(label + ": " + regions[i]);
            }

        } catch (Exception e) {
            fail("解析结果测试失败: " + e.getMessage());
        } finally {
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (Exception e) {
                }
            }
        }
    }
}