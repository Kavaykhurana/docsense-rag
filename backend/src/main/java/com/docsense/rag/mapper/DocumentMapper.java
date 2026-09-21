package com.docsense.rag.mapper;

import com.docsense.rag.dto.DocumentDto;
import com.docsense.rag.entity.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentDto toDto(Document d) {
        return new DocumentDto(
                d.getId(),
                d.getFilename(),
                d.getFileType().name(),
                d.getFileSize(),
                d.getStatus().name(),
                d.getPageCount(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }
}
