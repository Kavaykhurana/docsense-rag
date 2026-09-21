package com.docsense.rag.config;

import com.docsense.rag.rag.ChatCompletionClient;
import com.docsense.rag.rag.StubChatCompletionClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the offline {@link StubChatCompletionClient} only when no other
 * {@link ChatCompletionClient} bean exists. It backs off automatically once the
 * Gemini-backed implementation is provided in the final phase.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    @ConditionalOnMissingBean(ChatCompletionClient.class)
    public ChatCompletionClient stubChatCompletionClient() {
        return new StubChatCompletionClient();
    }
}
