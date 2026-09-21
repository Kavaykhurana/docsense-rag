package com.docsense.rag.service;

import com.docsense.rag.document.ChunkDraft;
import com.docsense.rag.document.ExtractedSection;
import com.docsense.rag.entity.Document;
import com.docsense.rag.entity.DocumentChunk;
import com.docsense.rag.entity.DocumentStatus;
import com.docsense.rag.rag.ChunkingService;
import com.docsense.rag.rag.EmbeddingService;
import com.docsense.rag.rag.VectorRepository;
import com.docsense.rag.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns extracted text into searchable, embedded chunks: chunk -> embed ->
 * persist metadata -> write pgvector embeddings -> mark the document COMPLETED.
 * Runs inside the caller's transaction; on any embedding/store failure the
 * document is marked FAILED and the exception is swallowed so the row persists
 * an honest final status (never left stuck in PROCESSING).
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final VectorRepository vectorRepository;

    public DocumentIngestionService(ChunkingService chunkingService,
                                    EmbeddingService embeddingService,
                                    DocumentChunkRepository chunkRepository,
                                    VectorRepository vectorRepository) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
        this.vectorRepository = vectorRepository;
    }

    @Transactional
    public void ingest(Document document, List<ExtractedSection> sections) {
        try {
            List<ChunkDraft> drafts = chunkingService.split(sections);
            if (drafts.isEmpty()) {
                document.setStatus(DocumentStatus.FAILED);
                return;
            }
            List<float[]> vectors = embeddingService.embed(
                    drafts.stream().map(ChunkDraft::content).toList());

            List<DocumentChunk> saved = new ArrayList<>(drafts.size());
            for (ChunkDraft d : drafts) {
                DocumentChunk c = new DocumentChunk();
                c.setDocumentId(document.getId());
                c.setChunkIndex(d.chunkIndex());
                c.setContent(d.content());
                c.setPageNumber(d.pageNumber());
                saved.add(chunkRepository.save(c));
            }
            // Ensure chunk rows are flushed so their generated ids are visible
            // to the native embedding updates below.
            chunkRepository.flush();

            for (int i = 0; i < saved.size(); i++) {
                vectorRepository.updateEmbedding(saved.get(i).getId(), vectors.get(i));
            }
            document.setStatus(DocumentStatus.COMPLETED);
        } catch (RuntimeException ex) {
            log.error("Ingestion failed for document {}", document.getId(), ex);
            document.setStatus(DocumentStatus.FAILED);
        }
    }
}
