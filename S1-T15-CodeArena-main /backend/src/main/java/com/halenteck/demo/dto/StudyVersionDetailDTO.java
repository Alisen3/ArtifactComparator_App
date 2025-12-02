package com.halenteck.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record StudyVersionDetailDTO(
        int versionNumber,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        JsonNode config
) {
}

