package com.ld.poetry.controller;

import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.service.FontSubsetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

/**
 * 字体子集化管理 — REST API
 * <p>
 * 提供 TTF 字体上传、在线切分为 WOFF2 子集、状态查询和清理功能。
 * 仅站长可操作。
 */
@RestController
@RequestMapping("/fontSubset")
@Slf4j
public class FontSubsetController {

    @Autowired
    private FontSubsetService fontSubsetService;

    /**
     * 上传 TTF 字体文件并执行子集化
     * 使用 cn-font-split 生成 font.css + 多个细粒度 woff2 分片
     */
    @PostMapping("/upload")
    @LoginCheck(0)
    public PoetryResult<Map<String, Object>> uploadAndSubset(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return PoetryResult.fail("请选择字体文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null
                || (!filename.toLowerCase().endsWith(".ttf") && !filename.toLowerCase().endsWith(".otf"))) {
            return PoetryResult.fail("仅支持 .ttf 或 .otf 格式的字体文件");
        }

        try {
            byte[] ttfData = file.getBytes();
            Path outputDir = fontSubsetService.getDefaultOutputDir();

            log.info("开始字体子集化: 文件={}, 大小={} bytes, 输出目录={}",
                    filename, ttfData.length, outputDir);

            Map<String, Object> result = fontSubsetService.subsetFont(ttfData, outputDir);
            return PoetryResult.success(result);
        } catch (Exception e) {
            log.error("字体子集化失败", e);
            return PoetryResult.fail("字体子集化失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前字体文件状态
     */
    @GetMapping("/status")
    @LoginCheck(0)
    public PoetryResult<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = fontSubsetService.getStatus();
            return PoetryResult.success(status);
        } catch (Exception e) {
            log.error("查询字体状态失败", e);
            return PoetryResult.fail("查询字体状态失败: " + e.getMessage());
        }
    }

    /**
     * 清理已生成的字体子集文件
     */
    @DeleteMapping("/clean")
    @LoginCheck(0)
    public PoetryResult<String> cleanSubsets() {
        try {
            boolean success = fontSubsetService.cleanSubsets();
            if (success) {
                return PoetryResult.success("字体子集文件已清理");
            } else {
                return PoetryResult.fail("部分文件清理失败，请检查日志");
            }
        } catch (Exception e) {
            log.error("清理字体子集失败", e);
            return PoetryResult.fail("清理失败: " + e.getMessage());
        }
    }
}
