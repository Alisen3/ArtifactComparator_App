// Dosya Yolu: demo/CreateQuestionDTO.java
package com.halenteck.demo.dto;

import java.util.List;
import com.halenteck.demo.QuestionType;

// Frontend'den gelen JSON'daki her bir "question" nesnesini temsil edecek.
public record CreateQuestionDTO(
        String questionText,
        QuestionType questionType, // MULTIPLE_CHOICE veya SHORT_ANSWER

        // Bu soruya ait şıkların listesi
        // questionType = SHORT_ANSWER ise bu liste boş veya null olabilir.
        List<CreateOptionDTO> options
) {
}