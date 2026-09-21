package com.docsense.rag.controller;

import com.docsense.rag.dto.ConversationDetailDto;
import com.docsense.rag.dto.ConversationDto;
import com.docsense.rag.dto.CreateConversationRequest;
import com.docsense.rag.security.UserPrincipal;
import com.docsense.rag.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Chat-history CRUD endpoints, all scoped to the authenticated user. */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ChatService chatService;

    public ConversationController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ConversationDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        return chatService.list(principal.getUserId());
    }

    @GetMapping("/{id}")
    public ConversationDetailDto detail(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return chatService.detail(principal.getUserId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDto create(@Valid @RequestBody CreateConversationRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return chatService.create(principal.getUserId(), request.title());
    }

    @PatchMapping("/{id}")
    public ConversationDto rename(@PathVariable Long id,
                                  @Valid @RequestBody CreateConversationRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return chatService.rename(principal.getUserId(), id, request.title());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        chatService.delete(principal.getUserId(), id);
    }
}
