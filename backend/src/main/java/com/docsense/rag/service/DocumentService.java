package com.docsense.rag.service;

import com.docsense.rag.document.ExtractionResult;
import com.docsense.rag.document.TextExtractionService;
import com.docsense.rag.dto.DocumentDto;
import com.docsense.rag.entity.Document;
import com.docsense.rag.entity.DocumentStatus;
import com.docsense.rag.entity.FileType;
import com.docsense.rag.exception.BadRequestException;
import com.docsense.rag.exception.ResourceNotFoundException;
import com.docsense.rag.mapper.DocumentMapper;
import com.docsense.rag.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Orchestrates upload validation and text extraction, persisting only document
 * metadata. The uploaded file is streamed and never written to disk, so the
 * original always stays on the user's computer (spec §4).
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final DocumentMapper documentMapper;
    private final long maxFileSizeBytes;

    public DocumentService(DocumentRepository documentRepository,
                           TextExtractionService textExtractionService,
                           DocumentMapper documentMapper,
                           @Value("${app.documents.max-file-size-bytes}") long maxFileSizeBytes) {
        this.documentRepository = documentRepository;
        this.textExtractionService = textExtractionService;
        this.documentMapper = documentMapper;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Transactional
    public DocumentDto upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The uploaded file is empty.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("A filename is required.");
        }
        // Keep only the basename to avoid any path traversal in stored metadata.
        String safeName = Paths.get(originalName).getFileName().toString();

        FileType type = detectType(safeName);

        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File exceeds the maximum allowed size of "
                    + (maxFileSizeBytes / (1024 * 1024)) + " MB.");
        }

        ExtractionResult result;
        try (InputStream in = file.getInputStream()) {
            result = textExtractionService.extract(type, in);
        } catch (IOException e) {
            throw new BadRequestException("The file could not be read.");
        }

        Document document = new Document();
        document.setUserId(userId);
        document.setFilename(safeName);
        document.setFileType(type);
        document.setFileSize(file.getSize());
        document.setPageCount(result.pageCount());
        // Extraction succeeded; embedding/chunking completes in a later phase.
        document.setStatus(DocumentStatus.PROCESSING);
        document = documentRepository.save(document);

        return documentMapper.toDto(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> list(Long userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(documentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDto get(Long userId, Long id) {
        return documentRepository.findByIdAndUserId(id, userId)
                .map(documentMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Document document = documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        // ON DELETE CASCADE removes associated chunks/embeddings (added in later phases).
        documentRepository.delete(document);
    }

    private FileType detectType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0) {
            throw new BadRequestException("Unsupported file type. Allowed: PDF, DOCX, TXT, Markdown.");
        }
        String ext = lower.substring(dot + 1);
        return switch (ext) {
            case "pdf" -> FileType.PDF;
            case "docx" -> FileType.DOCX;
            case "txt" -> FileType.TXT;
            case "md", "markdown" -> FileType.MD;
            default -> throw new BadRequestException(
                    "Unsupported file type '." + ext + "'. Allowed: PDF, DOCX, TXT, Markdown.");
        };
    }
}
