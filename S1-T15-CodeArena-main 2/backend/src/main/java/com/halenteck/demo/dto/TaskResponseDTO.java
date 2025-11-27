package com.halenteck.demo.dto;

import java.time.LocalDateTime;
import com.halenteck.demo.entity.ComparisonTaskEntity;

public record TaskResponseDTO(
        Long taskId,
        Long studyId,
        String studyTitle,
        boolean blinded, // --- NEW FIELD ---
        ComparisonTaskEntity.TaskStatus status,
        LocalDateTime createdAt,
        ArtifactSummaryDTO artifactA,
        ArtifactSummaryDTO artifactB
) {
}
