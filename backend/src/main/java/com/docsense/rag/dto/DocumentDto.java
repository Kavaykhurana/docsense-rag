package com.docsense.rag.dto;

import java.time.Instant;

/** Public representation of a document (no JPA entity exposure, spec §17). */
public record DocumentDto(
        Long id,
        String filename,
        String fileType,
        long fileSize,
        String status,
        Integer pageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
