package com.docsense.rag.config;

import com.docsense.rag.rag.EmbeddingService;
import com.docsense.rag.rag.StubEmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the deterministic {@link StubEmbeddingService} only when no other
 * {@link EmbeddingService} bean is present. Once the Gemini-backed embedding
 * service is added (final phase), it becomes the sole {@code EmbeddingService}
 * bean and this stub backs off automatically.
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService stubEmbeddingService(
            @Value("${app.embeddings.dimension}") int dimension) {
        return new StubEmbeddingService(dimension);
    }
}
