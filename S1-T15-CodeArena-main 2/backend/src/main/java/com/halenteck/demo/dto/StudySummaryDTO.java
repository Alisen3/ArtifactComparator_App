package com.halenteck.demo.dto;

public record StudySummaryDTO(
        Long id,
        String title,
        String description,
        boolean blinded, // YENİ EKLENEN ALAN
        QuizSummaryDTO competencyQuiz
) {
}