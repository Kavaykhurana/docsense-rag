package com.docsense.rag.document;

/**
 * A chunk produced by the chunker before persistence: text content, a document
 * ordering index, and the source page number (nullable when unavailable).
 */
public record ChunkDraft(String content, int chunkIndex, Integer pageNumber) {
}
