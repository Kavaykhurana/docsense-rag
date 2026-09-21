package com.docsense.rag.rag;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Orchestrates the retrieval-augmented generation step: retrieve relevant
 * chunks, hand them to the generative client as grounding context, and attach
 * per-source citations to the resulting answer.
 */
@Service
public class RagService {

    private static final int SNIPPET_LENGTH = 280;

    private final RetrievalService retrievalService;
    private final ChatCompletionClient chatCompletionClient;

    public RagService(RetrievalService retrievalService, ChatCompletionClient chatCompletionClient) {
        this.retrievalService = retrievalService;
        this.chatCompletionClient = chatCompletionClient;
    }

    public RagAnswer answer(Long userId, String question, Collection<Long> documentIds) {
        List<RetrievedChunk> chunks = retrievalService.retrieve(userId, question, documentIds);
        List<String> context = chunks.stream().map(RetrievedChunk::content).toList();
        String answer = chatCompletionClient.answer(question, context);
        List<Citation> citations = chunks.stream()
                .map(c -> Citation.from(c, SNIPPET_LENGTH))
                .toList();
        return new RagAnswer(answer, citations);
    }
}
