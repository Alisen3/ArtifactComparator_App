// Dosya Yolu: demo/QuizTakeDTO.java
package com.halenteck.demo.dto;

import java.util.List;

// Katılımcının alacağı kuis'in tamamını (sorular ve şıklar)
// frontend'e (React) göndermek için kullanılan ana DTO.
public record QuizTakeDTO(
        Long quizId,
        String title,
        String description,
        Integer durationInMinutes, // Issue #12 (Zaman Sınırı)
        List<QuizQuestionDTO> questions
) {
}