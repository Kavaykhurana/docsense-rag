package com.docsense.rag.document;

import java.util.List;

/** Result of text extraction: ordered sections plus optional page count. */
public record ExtractionResult(List<ExtractedSection> sections, Integer pageCount) {
}
