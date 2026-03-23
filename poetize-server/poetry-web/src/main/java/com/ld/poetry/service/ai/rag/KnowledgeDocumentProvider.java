package com.ld.poetry.service.ai.rag;

import com.ld.poetry.service.ai.rag.dto.KnowledgeSourceDocument;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentProvider {

    String sourceType();

    List<KnowledgeSourceDocument> listAllDocuments();

    Optional<KnowledgeSourceDocument> getDocument(String sourceId);
}
