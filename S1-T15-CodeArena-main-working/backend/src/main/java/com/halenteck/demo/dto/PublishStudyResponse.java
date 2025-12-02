package com.halenteck.demo.dto;

import java.time.LocalDateTime;

public record PublishStudyResponse(
        Long studyId,
        int versionNumber,
        LocalDateTime publishedAt,
        String message
) {
}


