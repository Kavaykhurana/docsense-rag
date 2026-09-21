package com.docsense.rag.document;

import com.docsense.rag.entity.FileType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Extracts text from plain-text and Markdown files (UTF-8). */
@Component
public class PlainTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(FileType type) {
        return type == FileType.TXT || type == FileType.MD;
    }

    @Override
    public ExtractionResult extract(InputStream content) throws IOException {
        String text = new String(content.readAllBytes(), StandardCharsets.UTF_8).strip();
        List<ExtractedSection> sections =
                text.isEmpty() ? List.of() : List.of(new ExtractedSection(text, null));
        return new ExtractionResult(sections, null);
    }
}
