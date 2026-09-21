package com.docsense.rag.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Production {@link EmbeddingService} backed by Google Gemini through Spring AI's
 * {@link EmbeddingModel} (configured {@code text-embedding-004} => 768 dims).
 * Active only when {@code app.ai.provider=gemini}.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GoogleGeminiEmbeddingService implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public GoogleGeminiEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(embeddingModel.embed(text));
        }
        return vectors;
    }

    @Override
    public int dimension() {
        return embeddingModel.dimensions();
    }
}
