package com.docsense.rag.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Local, deterministic stand-in for the Gemini embedding model. It hashes each
 * token into a fixed-size bag-of-words vector and L2-normalizes it, so cosine
 * similarity between texts behaves sensibly (shared words pull vectors
 * together). This exists only so the pgvector storage/retrieval pipeline can be
 * built and verified before an API key is provided; it is replaced by the real
 * Gemini-backed implementation and backs off automatically once that bean is
 * present (see EmbeddingConfig).
 */
public class StubEmbeddingService implements EmbeddingService {

    private final int dimension;

    public StubEmbeddingService(int dimension) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("embedding dimension must be positive");
        }
        this.dimension = dimension;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> out = new ArrayList<>(texts.size());
        for (String t : texts) {
            out.add(embedOne(t));
        }
        return out;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    private float[] embedOne(String text) {
        float[] v = new float[dimension];
        if (text == null || text.isBlank()) {
            return v;
        }
        for (String token : text.toLowerCase().split("\\W+")) {
            if (token.isEmpty()) {
                continue;
            }
            int idx = Math.floorMod(token.hashCode(), dimension);
            v[idx] += 1.0f;
        }
        double sum = 0.0;
        for (float f : v) {
            sum += (double) f * f;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] = (float) (v[i] / norm);
            }
        }
        return v;
    }
}
