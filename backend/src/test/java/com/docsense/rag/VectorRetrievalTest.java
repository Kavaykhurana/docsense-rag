package com.docsense.rag;

import com.docsense.rag.entity.User;
import com.docsense.rag.rag.RetrievedChunk;
import com.docsense.rag.rag.RetrievalService;
import com.docsense.rag.repository.UserRepository;
import com.docsense.rag.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the pgvector write + cosine-read path against the real database:
 * an uploaded document's chunks are embedded and stored, and a semantically
 * related query retrieves them — while a different user retrieves nothing.
 */
@SpringBootTest
@Transactional
class VectorRetrievalTest {

    @Autowired DocumentService documentService;
    @Autowired RetrievalService retrievalService;
    @Autowired UserRepository userRepository;

    private Long newUser() {
        User u = new User();
        u.setGoogleId("g-" + UUID.randomUUID());
        u.setEmail(UUID.randomUUID() + "@example.com");
        u.setName("Retrieval Tester");
        return userRepository.saveAndFlush(u).getId();
    }

    private static MockMultipartFile txt(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes());
    }

    @Test
    void storesEmbeddingsAndRetrievesNearestChunk() {
        Long userId = newUser();
        documentService.upload(userId, txt("fruit.txt",
                "Bananas are yellow curved fruit that are rich in potassium."));

        List<RetrievedChunk> hits = retrievalService.retrieve(userId, "yellow potassium fruit", null);

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).filename()).isEqualTo("fruit.txt");
        assertThat(hits.get(0).content()).containsIgnoringCase("Bananas");
        // Similarity derived from cosine distance stays within [0,1].
        assertThat(hits.get(0).similarity()).isBetween(0.0, 1.0);
    }

    @Test
    void retrievalIsScopedToOwningUser() {
        Long ownerId = newUser();
        Long otherId = newUser();
        documentService.upload(ownerId, txt("private.txt",
                "Confidential notes about quarterly revenue and margins."));

        assertThat(retrievalService.retrieve(ownerId, "quarterly revenue", null)).isNotEmpty();
        assertThat(retrievalService.retrieve(otherId, "quarterly revenue", null)).isEmpty();
    }

    @Test
    void documentFilterRestrictsResults() {
        Long userId = newUser();
        Long alpha = documentService.upload(userId, txt("alpha.txt",
                "Photosynthesis converts sunlight into chemical energy in plants.")).id();
        documentService.upload(userId, txt("beta.txt",
                "The Roman Empire expanded across the Mediterranean Sea.")).id();

        List<RetrievedChunk> hits = retrievalService.retrieve(userId, "sunlight plants energy", List.of(alpha));

        assertThat(hits).isNotEmpty();
        assertThat(hits).allSatisfy(h -> assertThat(h.documentId()).isEqualTo(alpha));
    }
}
