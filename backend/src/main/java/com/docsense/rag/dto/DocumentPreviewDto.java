package com.docsense.rag.dto;

import java.util.List;

/** Read-only view of a document's extracted + chunked text for the details page. */
public record DocumentPreviewDto(
        Long id,
        String filename,
        String status,
        List<Chunk> chunks) {

    public record Chunk(int chunkIndex, Integer pageNumber, String content) {
    }
}
