// Dosya Yolu: demo/dto/ParticipantWithScoresDTO.java
package com.halenteck.demo.dto;

import java.util.Map;

/**
 * Participant information with quiz scores and questionnaire answers
 * Used for filtering participants based on their quiz scores or questionnaire answers
 */
public record ParticipantWithScoresDTO(
        Long id,
        String name,
        String email,
        String skills,
        Integer yearsOfExperience,
        // Map of quizId -> score (percentage)
        Map<Long, Double> quizScores,
        // Map of questionId -> answer text or option text
        Map<Long, String> questionnaireAnswers
) {
}

