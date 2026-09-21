package com.docsense.rag.dto;

import com.docsense.rag.rag.Citation;

import java.time.Instant;
import java.util.List;

public record ChatResponse(
        Long conversationId,
        String answer,
        List<Citation> citations,
        Instant createdAt) {
}
