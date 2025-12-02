package com.halenteck.demo.dto;

public record CreateStudyTemplateRequest(
        Long studyId,
        String name,
        String description
) {
}

