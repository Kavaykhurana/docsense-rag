package com.docsense.rag.rag;

/**
 * A source reference attached to an answer, pointing at the exact document and
 * page a supporting passage came from (spec §6/§13). Also persisted with the
 * assistant message so citations survive chat-history reloads.
 */
public record Citation(
        Long documentId,
        String filename,
        Integer pageNumber,
        String snippet,
        double score) {

    public static Citation from(RetrievedChunk c, int maxSnippetLength) {
        String text = c.content();
        if (text != null && text.length() > maxSnippetLength) {
            text = text.substring(0, maxSnippetLength - 1) + "\u2026";
        }
        return new Citation(c.documentId(), c.filename(), c.pageNumber(), text, c.similarity());
    }
}
