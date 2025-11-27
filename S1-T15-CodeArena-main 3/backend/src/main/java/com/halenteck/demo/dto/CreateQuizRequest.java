// File: demo/dto/CreateQuizRequest.java
package com.halenteck.demo.dto;

import java.util.List;
import com.halenteck.demo.QuizType;

public record CreateQuizRequest(
        String title,
        String description,
        Integer durationInMinutes,
        QuizType type, // <--- NEW: Optional in JSON, defaults to COMPETENCY if null
        List<CreateQuestionDTO> questions
) {
}