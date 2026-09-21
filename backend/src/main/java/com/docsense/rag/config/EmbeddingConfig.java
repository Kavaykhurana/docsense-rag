package com.docsense.rag.config;

import com.docsense.rag.rag.EmbeddingService;
import com.docsense.rag.rag.StubEmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the deterministic {@link StubEmbeddingService} only when
 * {@code app.ai.provider=stub} (used by the offline test profile). In
 * production the provider is {@code gemini}, so the Gemini-backed
 * {@code EmbeddingService} bean is used instead and this stub is not created.
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub")
    public EmbeddingService stubEmbeddingService(
            @Value("${app.embeddings.dimension}") int dimension) {
        return new StubEmbeddingService(dimension);
    }
}
