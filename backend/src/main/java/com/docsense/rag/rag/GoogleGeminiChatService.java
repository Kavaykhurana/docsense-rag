package com.docsense.rag.rag;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Production {@link ChatCompletionClient} backed by Google Gemini through Spring
 * AI's {@link ChatModel} (configured {@code gemini-2.0-flash}). Instructed to
 * answer strictly from the retrieved, numbered context passages and to cite them
 * inline. Active only when {@code app.ai.provider=gemini}.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GoogleGeminiChatService implements ChatCompletionClient {

    private static final String SYSTEM_PROMPT = """
            You are a document question-answering assistant. Answer using ONLY the
            numbered context passages provided by the user. Follow these rules:
            - Ground every claim in the context; cite the passages you used inline
              with their bracket numbers, e.g. [1] or [2][3].
            - If the context does not contain the answer, say so plainly instead of
              guessing.
            - Never fabricate page numbers, quotations, or facts not present in the
              context.
            Be concise and directly answer the question.""";

    private final ChatModel chatModel;

    public GoogleGeminiChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String answer(String question, List<String> context) {
        if (context == null || context.isEmpty()) {
            return "I couldn't find any relevant content in your documents to answer that.";
        }
        StringBuilder sb = new StringBuilder("Context passages:\n\n");
        for (int i = 0; i < context.size(); i++) {
            sb.append('[').append(i + 1).append("] ")
              .append(context.get(i).strip()).append("\n\n");
        }
        sb.append("Question: ").append(question);

        List<Message> messages = List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(sb.toString()));

        ChatResponse response = chatModel.call(new Prompt(messages));
        String text = response.getResult().getOutput().getText();
        return (text == null || text.isBlank())
                ? "I couldn't find an answer in the provided context."
                : text.strip();
    }
}
