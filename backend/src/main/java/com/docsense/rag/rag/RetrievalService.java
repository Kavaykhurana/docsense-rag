package com.docsense.rag.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Embeds a query and returns the most similar chunks for a user, optionally
 * scoped to a selected set of documents (multi-document RAG, spec §8).
 */
@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;
    private final int defaultTopK;

    public RetrievalService(EmbeddingService embeddingService,
                            VectorRepository vectorRepository,
                            @Value("${app.rag.top-k}") int defaultTopK) {
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
        this.defaultTopK = defaultTopK;
    }

    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieve(Long userId, String query, Collection<Long> documentIds) {
        float[] queryVector = embeddingService.embed(List.of(query)).get(0);
        return vectorRepository.search(queryVector, userId, documentIds, defaultTopK);
    }
}
