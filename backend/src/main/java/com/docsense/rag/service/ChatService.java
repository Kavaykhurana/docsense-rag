package com.docsense.rag.service;

import com.docsense.rag.dto.ChatRequest;
import com.docsense.rag.dto.ChatResponse;
import com.docsense.rag.dto.ConversationDetailDto;
import com.docsense.rag.dto.ConversationDto;
import com.docsense.rag.dto.MessageDto;
import com.docsense.rag.entity.Conversation;
import com.docsense.rag.entity.Message;
import com.docsense.rag.entity.MessageRole;
import com.docsense.rag.exception.ResourceNotFoundException;
import com.docsense.rag.rag.RagAnswer;
import com.docsense.rag.rag.RagService;
import com.docsense.rag.repository.ConversationRepository;
import com.docsense.rag.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists chat history and drives the RAG answer flow. Every conversation and
 * message access is scoped to the authenticated user (spec §3).
 */
@Service
public class ChatService {

    private static final int TITLE_MAX = 60;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RagService ragService;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       RagService ragService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.ragService = ragService;
    }

    @Transactional
    public ChatResponse ask(Long userId, ChatRequest request) {
        Conversation conversation;
        if (request.conversationId() != null) {
            conversation = requireOwned(userId, request.conversationId());
        } else {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setTitle(deriveTitle(request.question()));
            conversation = conversationRepository.save(conversation);
        }

        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(request.question());
        messageRepository.save(userMessage);

        RagAnswer result = ragService.answer(userId, request.question(), request.documentIds());

        Message assistantMessage = new Message();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(result.answer());
        assistantMessage.setCitations(result.citations());
        assistantMessage = messageRepository.save(assistantMessage);

        // Touch the conversation so it bubbles to the top of the history list.
        conversationRepository.save(conversation);

        return new ChatResponse(
                conversation.getId(),
                assistantMessage.getContent(),
                assistantMessage.getCitations(),
                assistantMessage.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> list(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(c -> new ConversationDto(
                        c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt(),
                        messageRepository.countByConversationId(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailDto detail(Long userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        List<MessageDto> messages = messageRepository.findByConversationIdOrderByIdAsc(conversationId)
                .stream()
                .map(m -> new MessageDto(
                        m.getId(), m.getRole().name(), m.getContent(),
                        m.getCitations() == null ? List.of() : m.getCitations(),
                        m.getCreatedAt()))
                .toList();
        return new ConversationDetailDto(conversation.getId(), conversation.getTitle(), messages);
    }

    @Transactional
    public ConversationDto create(Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation = conversationRepository.save(conversation);
        return new ConversationDto(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt(), 0L);
    }

    @Transactional
    public ConversationDto rename(Long userId, Long conversationId, String title) {
        Conversation conversation = requireOwned(userId, conversationId);
        conversation.setTitle(title);
        conversation = conversationRepository.save(conversation);
        return new ConversationDto(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt(),
                messageRepository.countByConversationId(conversation.getId()));
    }

    @Transactional
    public void delete(Long userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        // ON DELETE CASCADE removes the messages.
        conversationRepository.delete(conversation);
    }

    private Conversation requireOwned(Long userId, Long conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    private static String deriveTitle(String question) {
        String t = question.strip();
        if (t.length() <= TITLE_MAX) {
            return t;
        }
        return t.substring(0, TITLE_MAX - 1) + "\u2026";
    }
}
