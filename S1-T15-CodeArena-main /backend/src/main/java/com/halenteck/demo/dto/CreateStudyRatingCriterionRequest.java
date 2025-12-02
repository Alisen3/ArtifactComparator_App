package com.halenteck.demo.dto;

public record CreateStudyRatingCriterionRequest(
        String name,
        String description,
        Double weight,
        Integer sortOrder
) {
}

