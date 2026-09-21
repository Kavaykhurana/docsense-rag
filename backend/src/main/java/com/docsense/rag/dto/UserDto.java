package com.docsense.rag.dto;

/**
 * Public representation of the authenticated user. JPA entities are never
 * exposed directly through the API (spec §17).
 */
public record UserDto(
        Long id,
        String email,
        String name,
        String picture
) {
}
