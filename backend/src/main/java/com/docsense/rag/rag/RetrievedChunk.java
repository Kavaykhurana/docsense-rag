package com.docsense.rag.rag;

/**
 * A chunk returned by vector similarity search, joined with its owning
 * document's filename for citation rendering. {@code distance} is the pgvector
 * cosine distance (0 = identical, 2 = opposite).
 */
public record RetrievedChunk(
        Long chunkId,
        Long documentId,
        String filename,
        Integer pageNumber,
        String content,
        double distance) {

    /** Convenience similarity in [0,1] where higher is more similar. */
    public double similarity() {
        return 1.0 - (distance / 2.0);
    }
}
