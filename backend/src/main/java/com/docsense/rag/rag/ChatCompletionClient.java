package com.docsense.rag.rag;

import java.util.List;

/**
 * Seam for the generative step of RAG: produce a grounded natural-language
 * answer to {@code question} using the supplied context snippets. The
 * production implementation calls Google Gemini (added in the final phase); a
 * local extractive stub is used until then so orchestration and citation wiring
 * can be verified without an API key.
 */
public interface ChatCompletionClient {

    /**
     * @param question the user's question
     * @param context  relevant passages retrieved for this question, ordered by
     *                 descending relevance (index 0 is most relevant)
     * @return the model's answer text
     */
    String answer(String question, List<String> context);
}
