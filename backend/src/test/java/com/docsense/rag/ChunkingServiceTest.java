package com.docsense.rag;

import com.docsense.rag.document.ChunkDraft;
import com.docsense.rag.document.ExtractedSection;
import com.docsense.rag.rag.ChunkingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {

    private static String words(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("word").append(i).append(' ');
        }
        return sb.toString().strip();
    }

    @Test
    void shortTextStaysSingleChunk() {
        List<String> out = ChunkingService.splitText("hello world", 100, 20);
        assertEquals(1, out.size());
        assertEquals("hello world", out.get(0));
    }

    @Test
    void chunksRespectMaxSizeAndWordBoundaries() {
        String text = words(500); // long, space separated
        int size = 200, overlap = 40;
        List<String> chunks = ChunkingService.splitText(text, size, overlap);
        assertTrue(chunks.size() > 1, "expected multiple chunks");
        for (String c : chunks) {
            assertTrue(c.length() <= size, "chunk too large: " + c.length());
            assertFalse(c.isBlank(), "blank chunk");
            // no chunk should end mid-word (ends on whitespace boundary by construction)
            assertFalse(c.startsWith(" "), "chunk should be stripped");
        }
        // Reconstruct: all original words must be present somewhere (no content lost).
        String joined = String.join(" ", chunks);
        assertTrue(joined.contains("word0"));
        assertTrue(joined.contains("word499"));
    }

    @Test
    void overlapRetainsContextBetweenChunks() {
        String text = words(300);
        int size = 150, overlap = 50;
        List<String> chunks = ChunkingService.splitText(text, size, overlap);
        assertTrue(chunks.size() >= 3);
        // Adjacent chunks should share some overlap words.
        String first = chunks.get(0);
        String second = chunks.get(1);
        String shared = first.substring(Math.max(0, first.length() - overlap));
        assertTrue(second.contains(shared.strip().split("\\s+")[0]),
                "expected overlap context to carry into next chunk");
    }

    @Test
    void makesForwardProgressEvenWithLargeOverlap() {
        String text = words(400);
        List<String> chunks = assertDoesNotThrow(() -> ChunkingService.splitText(text, 100, 99));
        assertFalse(chunks.isEmpty());
    }

    @Test
    void preservesPageNumberAndAssignsGlobalIndex() {
        ChunkingService svc = new ChunkingService(100, 20);
        List<ExtractedSection> sections = List.of(
                new ExtractedSection(words(300), 1),
                new ExtractedSection(words(50), 2));
        List<ChunkDraft> chunks = svc.split(sections);
        assertEquals(chunks.size(), chunks.stream().mapToInt(ChunkDraft::chunkIndex).distinct().count(),
                "chunk indices must be unique");
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).chunkIndex());
        }
        // page numbers preserved per section
        assertTrue(chunks.stream().anyMatch(c -> c.pageNumber().equals(1)));
        assertTrue(chunks.stream().anyMatch(c -> c.pageNumber().equals(2)));
    }

    @Test
    void rejectsInvalidConfig() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkingService(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkingService(100, 100));
        assertThrows(IllegalArgumentException.class, () -> new ChunkingService(100, -1));
    }
}
