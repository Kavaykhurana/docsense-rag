package com.docsense.rag.entity;

/**
 * Processing lifecycle of an uploaded document.
 * PENDING -> PROCESSING -> COMPLETED, or FAILED on error.
 */
public enum DocumentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}
