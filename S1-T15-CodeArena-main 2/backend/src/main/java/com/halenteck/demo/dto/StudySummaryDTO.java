// Dosya Yolu: demo/dto/StudySummaryDTO.java
package com.halenteck.demo.dto;

// React'e (frontend) Araştırmacının çalışmalarını listelerken
// güvenle göndermek için kullanılacak DTO.
// Bu, StudyEntity -> UserEntity -> StudyEntity sonsuz döngüsünü kırar.
public record StudySummaryDTO(
        Long id,
        String title,
        String description,

        // Bu çalışmaya atanmış bir kuis varsa, onun da
        // güvenli DTO'sunu (veya sadece ID'sini) buraya koyarız.
        // QuizSummaryDTO null olabilir (eğer kuis atanmamışsa).
        QuizSummaryDTO competencyQuiz
) {
}