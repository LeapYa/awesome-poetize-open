package com.ld.poetry.controller;

import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.SysAiConfig;
import com.ld.poetry.service.SysAiConfigService;
import com.ld.poetry.service.ai.Mem0Service;
import com.ld.poetry.utils.AESCryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 记忆管理控制器
 * 替代 Python 端 ai_chat_api.py 中的记忆管理 API
 *
 * 端点对照：
 * GET /ai/memory/list → get_user_memories
 * POST /ai/memory/search → search_memories
 * DELETE /ai/memory/{id} → delete_memory
 * DELETE /ai/memory/user/{uid} → delete_all_user_memories
 * POST /ai/memory/testConnection → test_memory_connection
 */
@RestController
@RequestMapping("/ai/memory")
public class AiMemoryController {

    private static final Logger logger = LoggerFactory.getLogger(AiMemoryController.class);

    @Autowired
    private Mem0Service mem0Service;

    @Autowired
    private SysAiConfigService sysAiConfigService;

    @Autowired
    private AESCryptoUtil aesCryptoUtil;

    /**
     * 获取用户所有记忆
     */
    @GetMapping("/list")
    public PoetryResult<Map<String, Object>> listMemories(
            @RequestParam String userId) {
        try {
            String apiKey = getMem0ApiKey();
            if (apiKey == null) {
                return PoetryResult.fail("Mem0 API Key 未配置");
            }

            Map<String, Object> result = mem0Service.listMemories(userId, apiKey);
            return PoetryResult.success(result);
        } catch (Exception e) {
            logger.error("获取记忆列表失败", e);
            return PoetryResult.fail("获取记忆列表失败: " + e.getMessage());
        }
    }

    /**
     * 搜索用户记忆
     */
    @PostMapping("/search")
    public PoetryResult<Map<String, Object>> searchMemories(@RequestBody Map<String, Object> request) {
        try {
            String apiKey = getMem0ApiKey();
            if (apiKey == null) {
                return PoetryResult.fail("Mem0 API Key 未配置");
            }

            String query = (String) request.getOrDefault("query", "");
            String userId = (String) request.getOrDefault("user_id", "anonymous");
            int limit = request.containsKey("limit") ? ((Number) request.get("limit")).intValue() : 5;

            Map<String, Object> result = mem0Service.searchMemories(query, userId, apiKey, limit);
            return PoetryResult.success(result);
        } catch (Exception e) {
            logger.error("搜索记忆失败", e);
            return PoetryResult.fail("搜索记忆失败: " + e.getMessage());
        }
    }

    /**
     * 删除单条记忆
     */
    @DeleteMapping("/{memoryId}")
    public PoetryResult<Map<String, Object>> deleteMemory(@PathVariable String memoryId) {
        try {
            String apiKey = getMem0ApiKey();
            if (apiKey == null) {
                return PoetryResult.fail("Mem0 API Key 未配置");
            }

            Map<String, Object> result = mem0Service.deleteMemory(memoryId, apiKey);
            return PoetryResult.success(result);
        } catch (Exception e) {
            logger.error("删除记忆失败", e);
            return PoetryResult.fail("删除记忆失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户所有记忆
     */
    @DeleteMapping("/user/{userId}")
    public PoetryResult<Map<String, Object>> deleteAllUserMemories(@PathVariable String userId) {
        try {
            String apiKey = getMem0ApiKey();
            if (apiKey == null) {
                return PoetryResult.fail("Mem0 API Key 未配置");
            }

            Map<String, Object> result = mem0Service.deleteAllUserMemories(userId, apiKey);
            return PoetryResult.success(result);
        } catch (Exception e) {
            logger.error("删除用户记忆失败", e);
            return PoetryResult.fail("删除用户记忆失败: " + e.getMessage());
        }
    }

    /**
     * 测试 Mem0 连接
     */
    @PostMapping("/testConnection")
    public PoetryResult<Map<String, Object>> testConnection() {
        try {
            String apiKey = getMem0ApiKey();
            if (apiKey == null) {
                return PoetryResult.fail("Mem0 API Key 未配置");
            }

            Map<String, Object> result = mem0Service.testConnection(apiKey);
            return PoetryResult.success(result);
        } catch (Exception e) {
            logger.error("测试 Mem0 连接失败", e);
            return PoetryResult.fail("测试连接失败: " + e.getMessage());
        }
    }

    /**
     * 从 AI 聊天配置中获取 Mem0 API Key
     */
    private String getMem0ApiKey() {
        SysAiConfig config = sysAiConfigService.getAiChatConfigInternal("default");
        if (config == null || config.getMem0ApiKey() == null || config.getMem0ApiKey().isBlank()) {
            return null;
        }

        String key = config.getMem0ApiKey();
        // 尝试 AES 解密
        if (!key.startsWith("m0-")) {
            String decrypted = aesCryptoUtil.decrypt(key);
            if (decrypted != null) {
                return decrypted;
            }
        }
        return key;
    }
}
