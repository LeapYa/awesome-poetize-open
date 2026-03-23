package com.ld.poetry.service.ai.rag;

import com.ld.poetry.utils.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RagVersionService {

    private static final String RAG_VERSION_KEY_PREFIX = "poetize:ai:rag:version:";

    private final RedisUtil redisUtil;

    public RagVersionService(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    public String getVersion(String indexName) {
        if (!StringUtils.hasText(indexName)) {
            return "0";
        }
        Object version = redisUtil.get(buildKey(indexName));
        return version != null ? version.toString() : "0";
    }

    public String bumpVersion(String indexName) {
        if (!StringUtils.hasText(indexName)) {
            return "0";
        }
        long version = redisUtil.incr(buildKey(indexName), 1L);
        return Long.toString(version);
    }

    private String buildKey(String indexName) {
        return RAG_VERSION_KEY_PREFIX + indexName;
    }
}
