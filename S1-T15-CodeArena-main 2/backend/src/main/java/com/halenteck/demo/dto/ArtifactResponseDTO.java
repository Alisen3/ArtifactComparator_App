package com.halenteck.demo.dto;

import java.time.LocalDateTime;

public record ArtifactResponseDTO(
        Long id,
        String fileName,
        String fileType,
        LocalDateTime uploadedAt,
        String uploaderName
) {
}