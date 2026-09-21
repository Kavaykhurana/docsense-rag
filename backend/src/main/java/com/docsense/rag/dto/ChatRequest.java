package com.docsense.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A question for the RAG pipeline. When {@code conversationId} is null a new
 * conversation is created; {@code documentIds} optionally restricts retrieval
 * to a selected subset of the user's documents (empty/null = all documents).
 */
public record ChatRequest(
        @NotBlank(message = "question must not be empty")
        @Size(max = 4000, message = "question must be at most 4000 characters")
        String question,
        Long conversationId,
        List<Long> documentIds) {
}
