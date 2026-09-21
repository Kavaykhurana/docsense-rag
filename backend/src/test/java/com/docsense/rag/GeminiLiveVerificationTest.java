package com.docsense.rag;

import com.docsense.rag.entity.User;
import com.docsense.rag.rag.EmbeddingService;
import com.docsense.rag.rag.RagAnswer;
import com.docsense.rag.rag.RagService;
import com.docsense.rag.repository.UserRepository;
import com.docsense.rag.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OPT-IN live verification of the real Google Gemini path (Spring AI). Runs only
 * when GEMINI_API_KEY is present in the environment, and under the default
 * (provider=gemini) profile. Uses the real database; each test rolls back.
 * Not part of the offline default suite (which uses the stub profile).
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
@Transactional
class GeminiLiveVerificationTest {

    @Autowired EmbeddingService embeddingService;
    @Autowired RagService ragService;
    @Autowired DocumentService documentService;
    @Autowired UserRepository userRepository;

    private Long newUser() {
        User u = new User();
        u.setGoogleId("g-" + UUID.randomUUID());
        u.setEmail(UUID.randomUUID() + "@example.com");
        u.setName("Gemini Tester");
        return userRepository.saveAndFlush(u).getId();
    }

    @Test
    void activeEmbeddingServiceIsGeminiAndProducesVectors() {
        assertThat(embeddingService).isInstanceOf(
                com.docsense.rag.rag.GoogleGeminiEmbeddingService.class);

        float[] v = embeddingService.embed(List.of("The quick brown fox jumps over the lazy dog.")).get(0);
        assertThat(v).hasSize(768);
        boolean nonZero = false;
        for (float f : v) {
            if (f != 0f) {
                nonZero = true;
                break;
            }
        }
        assertThat(nonZero).as("embedding is not all-zero").isTrue();
    }

    @Test
    void endToEndRealRagAnswerIsGroundedWithCitations() {
        Long userId = newUser();
        documentService.upload(userId, new MockMultipartFile("file", "policy.txt", "text/plain",
                ("Company vacation policy: full-time employees receive 20 days of paid time off per "
                        + "calendar year, and an additional 5 days after five years of service.").getBytes()));

        RagAnswer answer = ragService.answer(userId, "How many paid vacation days do employees get?", null);

        assertThat(answer.citations()).isNotEmpty();
        assertThat(answer.citations().get(0).filename()).isEqualTo("policy.txt");
        assertThat(answer.answer()).isNotBlank();
        // The offline stub prefixes with this exact marker; a real Gemini answer will not.
        assertThat(answer.answer()).doesNotStartWith("Based on your documents:");
        System.out.println("[GeminiLiveVerification] ANSWER >>> " + answer.answer());
    }
}
