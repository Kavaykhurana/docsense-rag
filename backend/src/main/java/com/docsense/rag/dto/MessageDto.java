package com.docsense.rag.dto;

import com.docsense.rag.rag.Citation;

import java.time.Instant;
import java.util.List;

public record MessageDto(
        Long id,
        String role,
        String content,
        List<Citation> citations,
        Instant createdAt) {
}
