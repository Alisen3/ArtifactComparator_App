package com.halenteck.demo.dto;

public record StudyTaskArtifactDTO(
        Long artifactId,
        String fileName,
        String mimeType,
        String alias,
        int position
) {
}

