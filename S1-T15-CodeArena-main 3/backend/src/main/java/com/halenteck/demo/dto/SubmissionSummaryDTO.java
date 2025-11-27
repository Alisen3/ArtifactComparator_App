// Dosya Yolu: demo/SubmissionSummaryDTO.java
package com.halenteck.demo.dto;

import java.time.LocalDateTime;

/**
 * Araştırmacıya (Researcher) bir kuis'in sonuçlarını (puanlarını)
 * göstermek için kullanılan DTO.
 * (GET /api/studies/{studyId}/quiz/submissions)
 */
public record SubmissionSummaryDTO(
        Long submissionId,    // 'QuizSubmissionEntity'nin ID'si
        Long participantId,   // Katılımcının ID'si
        String participantName, // Katılımcının Adı
        Double score,           // Hesaplanan puan (örn: 100.0)
        LocalDateTime submittedAt   // Teslim zamanı
) {
}