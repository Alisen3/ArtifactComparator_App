// Dosya Yolu: demo/dto/ParticipantFilterRequestDTO.java
package com.halenteck.demo.dto;

import java.util.Map;

/**
 * Request DTO for filtering participants
 */
public record ParticipantFilterRequestDTO(
        // Quiz score filters: Map of quizId -> minimum score (percentage)
        Map<Long, Double> minQuizScores,
        // Questionnaire answer filters: Map of questionId -> expected answer value
        Map<Long, String> questionnaireAnswers,
        // Experience level filter (e.g., "Senior Developer")
        String experienceLevel,
        // Minimum years of experience
        Integer minYearsOfExperience,
        // Skills filter (comma-separated or single skill)
        String skills
) {
}

