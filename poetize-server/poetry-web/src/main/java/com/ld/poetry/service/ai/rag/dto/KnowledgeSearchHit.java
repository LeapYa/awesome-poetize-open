package com.ld.poetry.service.ai.rag.dto;

import lombok.Data;

@Data
public class KnowledgeSearchHit {
    private Long id;
    private String documentId;
    private String sourceType;
    private String sourceId;
    private String title;
    private String content;
    private String visibilityScope;
    private String metadataJson;
    private Integer chunkIndex;
    private Integer chunkCount;
    private Double similarity;
}
