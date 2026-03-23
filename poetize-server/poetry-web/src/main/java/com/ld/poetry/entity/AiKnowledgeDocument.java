package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 知识文档分片。
 * 使用 MariaDB 库内向量函数进行检索，避免引入额外向量中间件。
 */
@Data
@TableName("ai_knowledge_document")
public class AiKnowledgeDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("index_name")
    private String indexName;

    @TableField("document_id")
    private String documentId;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private String sourceId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("visibility_scope")
    private String visibilityScope;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("content_hash")
    private String contentHash;

    @TableField("embedding_text")
    private String embeddingText;

    @TableField("sync_status")
    private String syncStatus;

    @TableField("last_error")
    private String lastError;

    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
