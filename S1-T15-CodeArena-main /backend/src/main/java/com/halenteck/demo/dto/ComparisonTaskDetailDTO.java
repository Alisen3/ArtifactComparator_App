// File: backend/src/main/java/com/halenteck/demo/dto/ComparisonTaskDetailDTO.java
package com.halenteck.demo.dto;

import com.halenteck.demo.entity.ComparisonTaskEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.UserRepository;


public record ComparisonTaskDetailDTO(
        Long taskId,
        String studyTitle,
        boolean blinded,
        ArtifactSummaryDTO artifactA,
        ArtifactSummaryDTO artifactB,
        ComparisonTaskEntity.TaskStatus status,
        String annotations,
        
        Double clarityA,
        Double relevanceA,
        Double accuracyA,
        String commentA,
        String highlightDataA,

        Double clarityB,
        Double relevanceB,
        Double accuracyB,
        String commentB,
        String highlightDataB
) {
    public ComparisonTaskDetailDTO(ComparisonTaskEntity task, UserRepository userRepository) {
        this(
                task.getId(),
                task.getStudy().getTitle(),
                task.getStudy().isBlinded(),
                // --- UPDATED: Pass uploader name ---
                createArtifactSummary(task.getArtifactA(), userRepository),
                createArtifactSummary(task.getArtifactB(), userRepository),
                // -----------------------------------
                task.getStatus(),
                task.getAnnotations(),
                task.getClarityA(),
                task.getRelevanceA(),
                task.getAccuracyA(),
                task.getCommentA(),
                task.getHighlightDataA(),
                task.getClarityB(),
                task.getRelevanceB(),
                task.getAccuracyB(),
                task.getCommentB(),
                task.getHighlightDataB()
        );
    }

 private static ArtifactSummaryDTO createArtifactSummary(
            com.halenteck.demo.entity.ArtifactEntity artifact,
            UserRepository userRepository) {
        String uploaderName = null;
        if (artifact.getOwnerId() != null) {
            UserEntity uploader = userRepository.findById(artifact.getOwnerId())
                    .orElse(null);
            uploaderName = uploader != null ? uploader.getName() : null;
        }
        return new ArtifactSummaryDTO(
                artifact.getId(),
                artifact.getFileName(),
                uploaderName
        );
    }

        
}
