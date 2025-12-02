// Dosya Yolu: demo/QuizSubmitRequest.java
package com.halenteck.demo.dto;

import java.util.List;

// Katılımcının cevaplarını göndermek için kullanacağı ana DTO
// (POST /api/studies/{studyId}/quiz/submit)
public record QuizSubmitRequest(
        List<AnswerSubmitDTO> answers
) {
}