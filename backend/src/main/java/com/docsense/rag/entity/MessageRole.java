package com.docsense.rag.entity;

/** Author of a chat message. Values align with the messages.role CHECK constraint. */
public enum MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
