package com.docsense.rag.repository;

import com.docsense.rag.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * All access is scoped by userId so a user can only ever reach their own
 * documents (spec §3 data isolation).
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Document> findByIdAndUserId(Long id, Long userId);
}
