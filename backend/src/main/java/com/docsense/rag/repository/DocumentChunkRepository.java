package com.docsense.rag.repository;

import com.docsense.rag.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    long countByDocumentId(Long documentId);

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);
}
