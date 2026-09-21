package com.docsense.rag;

import com.docsense.rag.entity.User;
import com.docsense.rag.repository.UserRepository;
import com.docsense.rag.security.UserPrincipal;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Phase 2 upload + text-extraction pipeline against the real
 * PostgreSQL/pgvector schema, using genuinely generated PDF/DOCX/TXT/MD files.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    private RequestPostProcessor asUser() {
        User user = new User();
        user.setGoogleId("g-" + UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setName("Test User");
        user = userRepository.saveAndFlush(user);
        UserPrincipal principal = new UserPrincipal(user, Map.of("sub", user.getGoogleId()));
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }

    private static byte[] samplePdf() throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (int i = 1; i <= 2; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Page " + i + ": the proposed architecture uses microservices for scalability.");
                    cs.endText();
                }
            }
            doc.save(bos);
            return bos.toByteArray();
        }
    }

    private static byte[] sampleDocx() throws Exception {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText("Retrieval augmented generation grounds answers in source text.");
            doc.write(bos);
            return bos.toByteArray();
        }
    }

    @Test
    void rejectsUnauthenticatedUpload() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadsPdfAndStoresMetadata() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "arch.pdf", "application/pdf", samplePdf()))
                        .with(asUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("arch.pdf"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andExpect(jsonPath("$.pageCount").value(2))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        mockMvc.perform(get("/api/documents").with(asUser()))
                .andExpect(status().isOk());
    }

    @Test
    void uploadsDocx() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "notes.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", sampleDocx()))
                        .with(asUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType").value("DOCX"))
                .andExpect(jsonPath("$.pageCount").doesNotExist());
    }

    @Test
    void uploadsTxtAndMarkdown() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "readme.txt", "text/plain",
                                "Plain text about pgvector similarity search.".getBytes()))
                        .with(asUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType").value("TXT"));

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "guide.md", "text/markdown",
                                "# Guide\nMarkdown content about embeddings.".getBytes()))
                        .with(asUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType").value("MD"));
    }

    @Test
    void rejectsUnsupportedType() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "script.exe", "application/octet-stream",
                                "binary".getBytes()))
                        .with(asUser()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]))
                        .with(asUser()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enforcesUserIsolation() throws Exception {
        // Upload as user A.
        // Simpler: verify a second user sees none of the first user's documents.
        RequestPostProcessor userA = asUser();
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "private.txt", "text/plain",
                                "secret content".getBytes()))
                        .with(userA))
                .andExpect(status().isCreated());
        // Because asUser() creates a NEW user each call, user B's list must be empty.
        mockMvc.perform(get("/api/documents").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
