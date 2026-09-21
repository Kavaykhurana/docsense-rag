package com.docsense.rag.rag;

import java.util.List;

/**
 * Deterministic, offline stand-in for the Gemini chat model. It performs a
 * simple extractive summary: quotes the first sentence of the most relevant
 * passage and appends a {@code [1]} citation marker. This is a placeholder for
 * the real generative call and backs off once a Gemini-backed
 * {@link ChatCompletionClient} bean is registered (see ChatClientConfig).
 */
public class StubChatCompletionClient implements ChatCompletionClient {

    @Override
    public String answer(String question, List<String> context) {
        if (context == null || context.isEmpty()) {
            return "I couldn't find any relevant content in your documents to answer that.";
        }
        String top = context.get(0).strip();
        String sentence = top.split("(?<=[.!?])\\s+", 2)[0];
        if (sentence.length() > 300) {
            sentence = sentence.substring(0, 297) + "...";
        }
        return "Based on your documents: " + sentence + " [1]";
    }
}
