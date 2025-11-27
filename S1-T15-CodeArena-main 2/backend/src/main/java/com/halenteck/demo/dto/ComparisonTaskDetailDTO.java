package com.halenteck.demo.dto;

import com.halenteck.demo.entity.ComparisonTaskEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.UserRepository;
import com.halenteck.demo.store.entity.StoreArtifactEntity; // YENİ IMPORT

public record ComparisonTaskDetailDTO(
        Long id,
        Long studyId,
        String studyTitle,
        boolean isBlinded,
        String participantName,
        ArtifactSummaryDTO artifactA,
        ArtifactSummaryDTO artifactB,
        String status,
        String annotations,
        Double clarityA, Double relevanceA, Double accuracyA, String commentA,
        Double clarityB, Double relevanceB, Double accuracyB, String commentB,
        String highlightDataA, String highlightDataB
) {
    public ComparisonTaskDetailDTO(ComparisonTaskEntity task, UserRepository userRepository) {
        this(
                task.getId(),
                task.getStudy().getId(),
                task.getStudy().getTitle(),
                task.getStudy().isBlinded(),
                task.getParticipant().getName(),
                mapArtifact(task.getArtifactA(), userRepository),
                mapArtifact(task.getArtifactB(), userRepository),
                task.getStatus().name(),
                task.getAnnotations(),
                task.getClarityA(), task.getRelevanceA(), task.getAccuracyA(), task.getCommentA(),
                task.getClarityB(), task.getRelevanceB(), task.getAccuracyB(), task.getCommentB(),
                task.getHighlightDataA(), task.getHighlightDataB()
        );
    }

    private static ArtifactSummaryDTO mapArtifact(StoreArtifactEntity artifact, UserRepository userRepo) {
        if (artifact == null) return null;
        String uploaderName = userRepo.findById(artifact.getOwnerId()).map(UserEntity::getName).orElse("Unknown");
        return new ArtifactSummaryDTO(artifact.getId(), artifact.getFilename(), uploaderName);
    }
}