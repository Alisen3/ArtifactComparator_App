package com.halenteck.demo.dto;

import java.time.LocalDateTime;

public record StudyRatingCriterionDTO(
        Long id,
        String name,
        String description,
        double weight,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

