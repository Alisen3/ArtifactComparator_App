package com.halenteck.demo.dto;

import java.time.LocalDateTime;

public record StudyTemplateDTO(
        Long id,
        String name,
        String description,
        LocalDateTime updatedAt,
        LocalDateTime lastUsedAt
) {
}

