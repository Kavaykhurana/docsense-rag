package com.docsense.rag.rag;

import java.util.List;

/**
 * Converts text into dense vectors for semantic search. The production
 * implementation is backed by Google Gemini (wired once GEMINI_API_KEY is
 * available); a deterministic local stub is used until then so the storage and
 * retrieval paths can be verified end-to-end without external credentials.
 */
public interface EmbeddingService {

    /** Returns one embedding vector per input text, in the same order. */
    List<float[]> embed(List<String> texts);

    /** Dimensionality of the vectors produced by this service. */
    int dimension();
}
