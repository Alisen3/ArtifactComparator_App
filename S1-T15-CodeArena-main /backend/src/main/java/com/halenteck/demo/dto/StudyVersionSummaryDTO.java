package com.halenteck.demo.dto;

import java.time.LocalDateTime;

public record StudyVersionSummaryDTO(
        int versionNumber,
        LocalDateTime createdAt,
        LocalDateTime publishedAt
) {
}

