package com.docsense.rag.exception;

/**
 * Standard API error body returned by {@link GlobalExceptionHandler}.
 * Never contains stack traces or internal details (spec §22).
 */
public record ApiError(
        int status,
        String error,
        String message
) {
}
