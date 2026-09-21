package com.docsense.rag.document;

import com.docsense.rag.entity.FileType;
import com.docsense.rag.exception.BadRequestException;
import com.docsense.rag.exception.DocumentProcessingException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Selects the appropriate {@link DocumentTextExtractor} for a file type and
 * guarantees the extraction produced non-empty text (spec §5/§6/§22).
 */
@Service
public class TextExtractionService {

    private final List<DocumentTextExtractor> extractors;

    public TextExtractionService(List<DocumentTextExtractor> extractors) {
        this.extractors = extractors;
    }

    public ExtractionResult extract(FileType type, InputStream content) {
        DocumentTextExtractor extractor = extractors.stream()
                .filter(e -> e.supports(type))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No extractor available for type " + type));

        ExtractionResult result;
        try {
            result = extractor.extract(content);
        } catch (DocumentProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw new DocumentProcessingException("Text extraction failed for this document.", e);
        }

        boolean empty = result.sections().isEmpty()
                || result.sections().stream().allMatch(s -> s.text() == null || s.text().isBlank());
        if (empty) {
            throw new DocumentProcessingException(
                    "No readable text found. The document may be empty or image-only (scanned PDFs are not supported).");
        }
        return result;
    }
}
