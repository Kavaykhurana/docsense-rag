package com.docsense.rag.document;

import com.docsense.rag.entity.FileType;

import java.io.IOException;
import java.io.InputStream;

/** Strategy for extracting text from a specific document format. */
public interface DocumentTextExtractor {

    boolean supports(FileType type);

    ExtractionResult extract(InputStream content) throws IOException;
}
