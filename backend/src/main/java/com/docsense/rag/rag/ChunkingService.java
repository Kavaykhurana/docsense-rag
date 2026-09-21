package com.docsense.rag.rag;

import com.docsense.rag.document.ChunkDraft;
import com.docsense.rag.document.ExtractedSection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted text into overlapping chunks that stay within a configurable
 * size, break on word boundaries where possible, and preserve per-page metadata
 * for citations (spec §7). Parameters come from application configuration.
 */
@Service
public class ChunkingService {

    private final int chunkSize;
    private final int chunkOverlap;

    public ChunkingService(@Value("${app.documents.chunk-size}") int chunkSize,
                           @Value("${app.documents.chunk-overlap}") int chunkOverlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunk-size must be positive");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunk-overlap must be >= 0 and < chunk-size");
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<ChunkDraft> split(List<ExtractedSection> sections) {
        List<ChunkDraft> chunks = new ArrayList<>();
        int index = 0;
        for (ExtractedSection section : sections) {
            String text = section.text();
            if (text == null || text.isBlank()) {
                continue;
            }
            for (String part : splitText(text.strip())) {
                chunks.add(new ChunkDraft(part, index++, section.pageNumber()));
            }
        }
        return chunks;
    }

    /** Visible for testing; uses the configured size/overlap. */
    public List<String> splitText(String text) {
        return splitText(text, chunkSize, chunkOverlap);
    }

    public static List<String> splitText(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        int n = text.length();
        if (n <= size) {
            chunks.add(text);
            return chunks;
        }
        int start = 0;
        while (start < n) {
            int end = Math.min(start + size, n);
            if (end < n) {
                // Back off to the last whitespace so we do not cut a word.
                int ws = lastIndexOfWhitespace(text, end);
                if (ws > start) {
                    end = ws;
                }
            }
            String chunk = text.substring(start, end).strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= n) {
                break;
            }
            int next = end - overlap;
            // Guarantee forward progress even with large overlap values.
            start = Math.max(start + 1, next);
        }
        return chunks;
    }

    private static int lastIndexOfWhitespace(String text, int fromExclusive) {
        for (int i = fromExclusive - 1; i >= 0; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
