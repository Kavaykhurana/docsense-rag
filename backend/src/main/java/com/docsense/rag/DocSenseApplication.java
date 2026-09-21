package com.docsense.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the DocSense RAG document Q&amp;A backend.
 */
@SpringBootApplication
public class DocSenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocSenseApplication.class, args);
    }
}
