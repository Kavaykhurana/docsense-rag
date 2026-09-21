package com.docsense.rag.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight liveness endpoint for the deployment platform's health check.
 * Public (unauthenticated) and reports database reachability so a green check
 * confirms the service and its pgvector datasource are both up.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        try {
            Integer dim = jdbcTemplate.queryForObject(
                    "select coalesce((select atttypmod from pg_attribute "
                            + "where attrelid = 'document_chunks'::regclass and attname = 'embedding'), -1)",
                    Integer.class);
            body.put("database", "UP");
            body.put("embeddingDimension", dim != null && dim > 0 ? dim : null);
        } catch (RuntimeException ex) {
            body.put("status", "DOWN");
            body.put("database", "DOWN");
        }
        return body;
    }
}
