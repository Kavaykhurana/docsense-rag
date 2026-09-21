package com.docsense.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @NotBlank(message = "title must not be empty")
        @Size(max = 500, message = "title must be at most 500 characters")
        String title) {
}
