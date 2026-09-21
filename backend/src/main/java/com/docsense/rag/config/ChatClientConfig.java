package com.docsense.rag.config;

import com.docsense.rag.rag.ChatCompletionClient;
import com.docsense.rag.rag.StubChatCompletionClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the offline {@link StubChatCompletionClient} only when
 * {@code app.ai.provider=stub} (test profile). In production the provider is
 * {@code gemini}, so the Gemini-backed {@code ChatCompletionClient} is used.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "stub")
    public ChatCompletionClient stubChatCompletionClient() {
        return new StubChatCompletionClient();
    }
}
