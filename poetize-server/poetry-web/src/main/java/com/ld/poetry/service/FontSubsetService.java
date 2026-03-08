package com.ld.poetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ld.poetry.utils.font.Woff2Encoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.*;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字体子集化服务 — 对应 Python split_font/font_subset.py 的 Java 移植
 * <p>
 * 将一个 TTF 字体文件切分为 4 个 woff2 子集:
 * <ul>
 * <li>base — ASCII 32-126 + 中文标点</li>
 * <li>level1 — 国家一级常用汉字 (~3500个)</li>
 * <li>level2 — 国家二级常用汉字 (~3000个)</li>
 * <li>other — 剩余所有字符</li>
 * </ul>
 * 同时生成 unicode_ranges.json 供前端 font-loader 使用.
 * 
 * @author LeapYa
 * @since 2026-03-06
 */
@Slf4j
@Service
public class FontSubsetService {

    /** 中文标点符号 (与 Python 版本一致) */
    private static final char[] CHINESE_PUNCTUATIONS = {
            '·', '—', '\u2018', '\u2019', '\u201C', '\u201D', '…',
            '。', '《', '》', '【', '】', '！', '，', '？', '～'
    };

    @Value("${local.uploadUrl:/app/static/}")
    private String uploadUrl;

    @Value("${local.downloadUrl:}")
    private String downloadUrl;

    /** 一级常用汉字列表 */
    private List<Character> level1Chars;
    /** 二级常用汉字列表 */
    private List<Character> level2Chars;

    @PostConstruct
    public void init() {
        try {
            level1Chars = readCharset("font/level-1.txt");
            level2Chars = readCharset("font/level-2.txt");
            log.info("字体子集化服务初始化完成: 一级汉字 {} 个, 二级汉字 {} 个",
                    level1Chars.size(), level2Chars.size());
        } catch (IOException e) {
            log.error("加载汉字表失败", e);
            level1Chars = List.of();
            level2Chars = List.of();
        }
    }

    /**
     * 执行字体子集化
     *
     * @param ttfData   上传的 TTF 字体文件字节
     * @param outputDir 输出目录 (font_chunks)
     * @return 处理结果摘要
     */
    public Map<String, Object> subsetFont(byte[] ttfData, Path outputDir) throws IOException {
        long startTime = System.currentTimeMillis();

        // 1. 创建输出目录
        Files.createDirectories(outputDir);

        // 2. 解析字体, 提取所有字符
        TrueTypeFont ttf;
        try (RandomAccessReadBuffer rar = new RandomAccessReadBuffer(ttfData)) {
            TTFParser parser = new TTFParser();
            ttf = parser.parse(rar);
        }

        Set<Character> allChars = extractAllChars(ttf);
        log.info("原始字体中的字符总数: {} 个", allChars.size());

        // 3. 分组
        List<Character> baseChars = buildBaseChars(allChars);
        List<Character> level1Filtered = filterChars(level1Chars, allChars, Set.copyOf(baseChars), Set.of());
        List<Character> level2Filtered = filterChars(level2Chars, allChars, Set.copyOf(baseChars),
                Set.copyOf(level1Filtered));
        List<Character> otherChars = buildOtherChars(allChars, baseChars, level1Filtered, level2Filtered);

        log.info("分组结果 — base: {}, level1: {}, level2: {}, other: {}",
                baseChars.size(), level1Filtered.size(), level2Filtered.size(), otherChars.size());

        // 4. 为每组生成子集 woff2
        Map<String, Long> fileSizes = new LinkedHashMap<>();
        fileSizes.put("font.base.woff2", createSubsetWoff2(ttfData, baseChars, outputDir.resolve("font.base.woff2")));
        fileSizes.put("font.level1.woff2",
                createSubsetWoff2(ttfData, level1Filtered, outputDir.resolve("font.level1.woff2")));
        fileSizes.put("font.level2.woff2",
                createSubsetWoff2(ttfData, level2Filtered, outputDir.resolve("font.level2.woff2")));
        fileSizes.put("font.other.woff2",
                createSubsetWoff2(ttfData, otherChars, outputDir.resolve("font.other.woff2")));

        // 5. 生成 unicode_ranges.json
        Map<String, List<String>> unicodeRanges = new LinkedHashMap<>();
        unicodeRanges.put("base", toUnicodeRangeStrings(baseChars));
        unicodeRanges.put("level1", toUnicodeRangeStrings(level1Filtered));
        unicodeRanges.put("level2", toUnicodeRangeStrings(level2Filtered));
        unicodeRanges.put("other", toUnicodeRangeStrings(otherChars));

        ObjectMapper mapper = new ObjectMapper();
        Path jsonPath = outputDir.resolve("unicode_ranges.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), unicodeRanges);

        ttf.close();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("字体子集化完成, 耗时 {} ms", elapsed);

        // 6. 返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("elapsedMs", elapsed);
        result.put("originalSize", ttfData.length);
        result.put("totalChars", allChars.size());
        result.put("groups", Map.of(
                "base", baseChars.size(),
                "level1", level1Filtered.size(),
                "level2", level2Filtered.size(),
                "other", otherChars.size()));
        result.put("fileSizes", fileSizes);
        result.put("outputDir", outputDir.toString());
        return result;
    }

    /**
     * 获取默认的 font_chunks 输出目录
     */
    public Path getDefaultOutputDir() {
        // 部署到静态资源目录下的 assets/font_chunks/
        String basePath = uploadUrl.endsWith("/") ? uploadUrl : uploadUrl + "/";
        return Path.of(basePath, "assets", "font_chunks");
    }

    /**
     * 获取当前字体文件状态
     */
    public Map<String, Object> getStatus() {
        Path outputDir = getDefaultOutputDir();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("outputDir", outputDir.toString());

        String[] expectedFiles = { "font.base.woff2", "font.level1.woff2", "font.level2.woff2", "font.other.woff2",
                "unicode_ranges.json" };
        Map<String, Object> files = new LinkedHashMap<>();
        boolean allExist = true;

        for (String name : expectedFiles) {
            Path p = outputDir.resolve(name);
            if (Files.exists(p)) {
                try {
                    files.put(name, Map.of("exists", true, "size", Files.size(p)));
                } catch (IOException e) {
                    files.put(name, Map.of("exists", true, "size", -1));
                }
            } else {
                files.put(name, Map.of("exists", false));
                allExist = false;
            }
        }

        status.put("files", files);
        status.put("ready", allExist);
        return status;
    }

    /**
     * 清理已生成的字体子集文件
     */
    public boolean cleanSubsets() {
        Path outputDir = getDefaultOutputDir();
        if (!Files.exists(outputDir)) {
            return true;
        }
        String[] filesToDelete = { "font.base.woff2", "font.level1.woff2", "font.level2.woff2", "font.other.woff2",
                "unicode_ranges.json" };
        boolean success = true;
        for (String name : filesToDelete) {
            try {
                Files.deleteIfExists(outputDir.resolve(name));
            } catch (IOException e) {
                log.error("删除文件失败: {}", name, e);
                success = false;
            }
        }
        return success;
    }

    // ==================== 内部方法 ====================

    /** 从 classpath 读取汉字表 (每行一个字符) */
    private List<Character> readCharset(@org.springframework.lang.NonNull String resourcePath) throws IOException {
        List<Character> chars = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (char c : line.trim().toCharArray()) {
                    if (c >= '\u4E00' && c <= '\u9FFF') { // 只保留 CJK 基本汉字
                        chars.add(c);
                    }
                }
            }
        }
        return chars;
    }

    /** 从字体 cmap 表提取所有字符 */
    private Set<Character> extractAllChars(TrueTypeFont ttf) throws IOException {
        Set<Character> chars = new HashSet<>();
        CmapTable cmapTable = ttf.getCmap();
        if (cmapTable == null)
            return chars;

        for (CmapSubtable subtable : cmapTable.getCmaps()) {
            // 遍历 BMP 平面 (U+0020 ~ U+FFFF)
            for (int code = 32; code <= 0xFFFF; code++) {
                int gid = subtable.getGlyphId(code);
                if (gid > 0) {
                    chars.add((char) code);
                }
            }
        }
        return chars;
    }

    /** 构建基础字符集: ASCII 32-126 + 中文标点 */
    private List<Character> buildBaseChars(Set<Character> allChars) {
        List<Character> base = new ArrayList<>();
        // ASCII 可打印字符
        for (int i = 32; i < 127; i++) {
            base.add((char) i);
        }
        // 中文标点
        for (char c : CHINESE_PUNCTUATIONS) {
            if (allChars.contains(c) && !base.contains(c)) {
                base.add(c);
            }
        }
        return base;
    }

    /** 过滤字符: 确保唯一性和字体中存在 */
    private List<Character> filterChars(List<Character> source, Set<Character> allChars,
            Set<Character> excludeA, Set<Character> excludeB) {
        List<Character> filtered = new ArrayList<>();
        Set<Character> seen = new HashSet<>();
        for (char c : source) {
            if (allChars.contains(c) && !excludeA.contains(c) && !excludeB.contains(c) && seen.add(c)) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    /** 构建 other 字符集 */
    private List<Character> buildOtherChars(Set<Character> allChars, List<Character> base,
            List<Character> level1, List<Character> level2) {
        Set<Character> used = new HashSet<>();
        used.addAll(base);
        used.addAll(level1);
        used.addAll(level2);
        return allChars.stream()
                .filter(c -> !used.contains(c))
                .sorted()
                .collect(Collectors.toList());
    }

    /** 使用 FontBox TTFSubsetter 创建子集 TTF, 然后编码为 WOFF2 */
    private long createSubsetWoff2(byte[] originalTtf, List<Character> chars, Path outputPath) throws IOException {
        log.info("正在生成 {}...", outputPath.getFileName());

        // 使用 FontBox TTFSubsetter
        TrueTypeFont ttf;
        try (RandomAccessReadBuffer rar = new RandomAccessReadBuffer(originalTtf)) {
            TTFParser parser = new TTFParser();
            ttf = parser.parse(rar);
        }

        TTFSubsetter subsetter = new TTFSubsetter(ttf);
        for (char c : chars) {
            subsetter.add((int) c);
        }

        // 输出子集 TTF 到内存
        ByteArrayOutputStream subsetTtfStream = new ByteArrayOutputStream();
        subsetter.writeToStream(subsetTtfStream);
        byte[] subsetTtfBytes = subsetTtfStream.toByteArray();

        ttf.close();

        // TTF → WOFF2
        byte[] woff2Bytes = Woff2Encoder.encode(subsetTtfBytes);

        // 写出
        Files.write(outputPath, woff2Bytes);
        log.info("生成完成: {} ({} bytes)", outputPath.getFileName(), woff2Bytes.length);
        return woff2Bytes.length;
    }

    /** 将字符列表转换为 Unicode 范围字符串列表 (合并相邻范围) */
    List<String> toUnicodeRangeStrings(List<Character> chars) {
        if (chars.isEmpty())
            return List.of();

        List<Integer> codes = chars.stream()
                .map(c -> (int) c)
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        // 合并相邻范围
        List<int[]> ranges = new ArrayList<>();
        int start = codes.get(0);
        int end = start;
        for (int i = 1; i < codes.size(); i++) {
            int code = codes.get(i);
            if (code == end + 1) {
                end = code;
            } else {
                ranges.add(new int[] { start, end });
                start = code;
                end = code;
            }
        }
        ranges.add(new int[] { start, end });

        // 转换为字符串
        return ranges.stream()
                .map(r -> r[0] == r[1]
                        ? String.format("U+%04X", r[0])
                        : String.format("U+%04X-%04X", r[0], r[1]))
                .collect(Collectors.toList());
    }
}
