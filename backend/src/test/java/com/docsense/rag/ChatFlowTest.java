package com.docsense.rag;

import com.docsense.rag.entity.User;
import com.docsense.rag.repository.UserRepository;
import com.docsense.rag.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end chat flow over the real pipeline (upload -> RAG answer -> history
 * persistence), using the local embedding/chat stubs. Confirms grounded answers
 * carry citations, conversations/messages persist and reload, validation and
 * per-user isolation hold.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ChatFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ObjectMapper objectMapper;

    private RequestPostProcessor asUser() {
        User user = new User();
        user.setGoogleId("g-" + UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setName("Chat Tester");
        user = userRepository.saveAndFlush(user);
        UserPrincipal principal = new UserPrincipal(user, Map.of("sub", user.getGoogleId()));
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }

    private Long uploadDoc(RequestPostProcessor user, String name, String content) throws Exception {
        String body = mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", name, "text/plain", content.getBytes()))
                        .with(user))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void asksQuestionAndGetGroundedAnswerWithCitations() throws Exception {
        RequestPostProcessor user = asUser();
        Long docId = uploadDoc(user, "recipe.txt",
                "To bake the cake, preheat the oven to 180 degrees and mix flour with eggs.");

        String body = mockMvc.perform(post("/api/chat").with(user).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How do I bake the cake?\",\"documentIds\":[" + docId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.citations[0].filename").value("recipe.txt"))
                .andReturn().getResponse().getContentAsString();

        JsonNode chat = objectMapper.readTree(body);
        long conversationId = chat.get("conversationId").asLong();

        mockMvc.perform(get("/api/conversations").with(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conversationId))
                .andExpect(jsonPath("$[0].messageCount").value(2));

        mockMvc.perform(get("/api/conversations/" + conversationId).with(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.messages[1].citations[0].documentId").value(docId));
    }

    @Test
    void continuesExistingConversation() throws Exception {
        RequestPostProcessor user = asUser();
        uploadDoc(user, "notes.txt", "The capital city hosts the annual trade fair every spring.");

        String first = mockMvc.perform(post("/api/chat").with(user).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What happens every spring?\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long conversationId = objectMapper.readTree(first).get("conversationId").asLong();

        mockMvc.perform(post("/api/chat").with(user).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"And where is the fair held?\",\"conversationId\":" + conversationId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId));

        mockMvc.perform(get("/api/conversations/" + conversationId).with(user))
                .andExpect(jsonPath("$.messages.length()").value(4));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/chat").with(asUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void conversationIsPrivateToItsOwner() throws Exception {
        RequestPostProcessor owner = asUser();
        String body = mockMvc.perform(post("/api/chat").with(owner).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Is this visible to others?\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long conversationId = objectMapper.readTree(body).get("conversationId").asLong();

        mockMvc.perform(get("/api/conversations").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/conversations/" + conversationId).with(asUser()))
                .andExpect(status().isNotFound());
    }
}
