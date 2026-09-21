package com.docsense.rag.rag;

import java.util.List;

/** A generated answer together with the source citations that back it. */
public record RagAnswer(String answer, List<Citation> citations) {
}
