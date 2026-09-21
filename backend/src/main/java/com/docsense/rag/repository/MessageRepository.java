package com.docsense.rag.repository;

import com.docsense.rag.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);

    long countByConversationId(Long conversationId);
}
