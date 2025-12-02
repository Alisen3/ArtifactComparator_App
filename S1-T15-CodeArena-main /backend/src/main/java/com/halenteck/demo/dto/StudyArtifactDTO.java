package com.halenteck.demo.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record StudyArtifactDTO(
        Long id,
        Long artifactId,
        String fileName,
        String mimeType,
        String alias,
        String ownerName,
        Instant artifactCreatedAt,
        LocalDateTime addedAt
) {
}

