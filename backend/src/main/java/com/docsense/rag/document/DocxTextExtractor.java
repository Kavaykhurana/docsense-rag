package com.docsense.rag.document;

import com.docsense.rag.entity.FileType;
import com.docsense.rag.exception.DocumentProcessingException;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Extracts text from .docx (Office Open XML) files using Apache POI. */
@Component
public class DocxTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(FileType type) {
        return type == FileType.DOCX;
    }

    @Override
    public ExtractionResult extract(InputStream content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(content);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            List<ExtractedSection> sections =
                    (text == null || text.isBlank())
                            ? List.of()
                            : List.of(new ExtractedSection(text.strip(), null));
            // Word has no fixed pagination; page numbers are intentionally unavailable.
            return new ExtractionResult(sections, null);
        } catch (IOException e) {
            throw new DocumentProcessingException("Could not read the DOCX. The file may be corrupted.", e);
        }
    }
}
