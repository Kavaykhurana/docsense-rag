package com.docsense.rag.exception;

/** Generic 400-type application exception with a client-safe message. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
