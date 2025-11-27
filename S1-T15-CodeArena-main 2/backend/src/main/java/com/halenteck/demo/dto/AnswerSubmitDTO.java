// Dosya Yolu: demo/AnswerSubmitDTO.java
package com.halenteck.demo.dto;

// Katılımcının 'POST /submit' isteğinde göndereceği
// JSON listesindeki her bir cevabı temsil eder.
public record AnswerSubmitDTO(
        Long questionId,
        Long selectedOptionId, // Eğer çoktan seçmeli ise
        String answerText     // Eğer kısa cevaplı ise
) {
}