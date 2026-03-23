package com.ld.poetry.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ld.poetry.entity.AiKnowledgeDocument;
import com.ld.poetry.service.ai.rag.dto.KnowledgeSearchHit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {

    List<KnowledgeSearchHit> searchSimilarChunks(@Param("indexName") String indexName,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("visibilityScope") String visibilityScope,
            @Param("topK") int topK,
            @Param("distanceThreshold") double distanceThreshold);

    List<AiKnowledgeDocument> selectChunksByDocumentRange(@Param("indexName") String indexName,
            @Param("documentId") String documentId,
            @Param("startChunkIndex") int startChunkIndex,
            @Param("endChunkIndex") int endChunkIndex);

    Long countDistinctDocuments(@Param("indexName") String indexName,
            @Param("visibilityScope") String visibilityScope);

    Long countChunks(@Param("indexName") String indexName,
            @Param("visibilityScope") String visibilityScope);

    LocalDateTime findLastSyncTime(@Param("indexName") String indexName,
            @Param("visibilityScope") String visibilityScope);

    List<AiKnowledgeDocument> selectRecentDocuments(@Param("indexName") String indexName,
            @Param("visibilityScope") String visibilityScope,
            @Param("limit") int limit);
}
