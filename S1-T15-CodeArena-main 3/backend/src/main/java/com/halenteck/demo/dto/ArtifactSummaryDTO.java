// File: backend/src/main/java/com/halenteck/demo/dto/ArtifactSummaryDTO.java
package com.halenteck.demo.dto;

public record ArtifactSummaryDTO(
        Long id,
        String fileName,
        String uploaderName // <--- NEW FIELD
) {
}
