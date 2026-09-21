package com.docsense.rag.dto;

import java.util.List;

public record ConversationDetailDto(
        Long id,
        String title,
        List<MessageDto> messages) {
}
