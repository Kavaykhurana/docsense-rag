package com.docsense.rag.document;

/**
 * A contiguous piece of extracted text together with its originating page
 * number (nullable when the format has no fixed pagination, e.g. DOCX/TXT).
 * Preserving page metadata enables accurate citations (spec §6/§13).
 */
public record ExtractedSection(String text, Integer pageNumber) {
}
