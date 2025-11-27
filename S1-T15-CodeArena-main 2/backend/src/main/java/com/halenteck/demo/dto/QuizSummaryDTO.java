// Dosya Yolu: demo/QuizSummaryDTO.java
package com.halenteck.demo.dto;

import java.time.LocalDateTime;

/**
 * Bir kuis'in özet bilgilerini frontend'e (React) göndermek için
 * kullanılan Veri Transfer Nesnesi (DTO).
 * (GET /api/quizzes/my-quizzes)
 */
public record QuizSummaryDTO(
        Long id,
        String title,
        String description,
        Integer durationInMinutes, // Issue #12
        LocalDateTime createdAt,
        int questionCount // Bu kuis'de kaç soru olduğunu gösterir
) {
}