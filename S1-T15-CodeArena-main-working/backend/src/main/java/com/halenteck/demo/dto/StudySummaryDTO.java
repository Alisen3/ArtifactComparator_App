// Dosya Yolu: demo/dto/StudySummaryDTO.java
package com.halenteck.demo.dto;

// React'e (frontend) Araştırmacının çalışmalarını listelerken
// güvenle göndermek için kullanılacak DTO.
// Bu, StudyEntity -> UserEntity -> StudyEntity sonsuz döngüsünü kırar.
import com.halenteck.demo.entity.StudyStatus;
import com.halenteck.demo.permission.StudyCollaboratorRole;

import java.time.LocalDateTime;

public record StudySummaryDTO(
        Long id,
        String title,
        String description,
        boolean blinded,
        QuizSummaryDTO competencyQuiz,
        StudyCollaboratorRole currentRole,
        StudyPermissionDTO permissions,
        StudyStatus status,
        Integer latestPublishedVersion,
        int nextVersionNumber,
        boolean hasUnpublishedChanges,
        LocalDateTime accessWindowStart,
        LocalDateTime accessWindowEnd
) {
}