package com.docsense.rag.controller;

import com.docsense.rag.dto.DocumentDto;
import com.docsense.rag.security.UserPrincipal;
import com.docsense.rag.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Thin REST controller for document operations (business logic in the service). */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto upload(@RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.upload(principal.getUserId(), file);
    }

    @GetMapping
    public List<DocumentDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        return documentService.list(principal.getUserId());
    }

    @GetMapping("/{id}")
    public DocumentDto get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.get(principal.getUserId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        documentService.delete(principal.getUserId(), id);
    }
}
