package com.docsense.rag.controller;

import com.docsense.rag.dto.ChatRequest;
import com.docsense.rag.dto.ChatResponse;
import com.docsense.rag.security.UserPrincipal;
import com.docsense.rag.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Question-answering endpoint (grounded RAG). Business logic lives in the service. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse ask(@Valid @RequestBody ChatRequest request,
                            @AuthenticationPrincipal UserPrincipal principal) {
        return chatService.ask(principal.getUserId(), request);
    }
}
