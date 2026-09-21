package com.docsense.rag.document;

import com.docsense.rag.entity.FileType;
import com.docsense.rag.exception.DocumentProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Extracts text per page from PDFs using Apache PDFBox, preserving page numbers. */
@Component
public class PdfTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(FileType type) {
        return type == FileType.PDF;
    }

    @Override
    public ExtractionResult extract(InputStream content) throws IOException {
        try (PDDocument document = PDDocument.load(content)) {
            int pages = document.getNumberOfPages();
            List<ExtractedSection> sections = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                if (text != null && !text.isBlank()) {
                    sections.add(new ExtractedSection(text.strip(), page));
                }
            }
            return new ExtractionResult(sections, pages);
        } catch (IOException e) {
            throw new DocumentProcessingException("Could not read the PDF. The file may be corrupted.", e);
        }
    }
}
