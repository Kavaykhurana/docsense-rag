package com.docsense.rag.exception;

/** Thrown when document extraction/processing fails. Maps to HTTP 422. */
public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
