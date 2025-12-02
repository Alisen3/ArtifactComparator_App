package com.halenteck.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudyTaskDefinitionDTO(
        Long id,
        String instructions,
        int sortOrder,
        List<StudyTaskArtifactDTO> artifacts,
        List<StudyRatingCriterionDTO> ratingCriteria,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

