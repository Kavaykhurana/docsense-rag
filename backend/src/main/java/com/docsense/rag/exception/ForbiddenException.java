package com.docsense.rag.exception;

/** Thrown when a user attempts to access another user's data. Maps to HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
