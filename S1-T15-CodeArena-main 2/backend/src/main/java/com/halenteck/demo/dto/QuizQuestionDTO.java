// Dosya Yolu: demo/QuizQuestionDTO.java
package com.halenteck.demo.dto;

import com.halenteck.demo.QuestionType;
import java.util.List;

// Katılımcıya gönderilecek GÜVENLİ soru DTO'su
public record QuizQuestionDTO(
        Long id,
        String questionText,
        QuestionType questionType,
        List<QuizOptionDTO> options // Güvenli DTO listesi
) {
}