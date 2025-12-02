package com.halenteck.demo.dto;

public record UpdateStudyRatingCriterionRequest(
        String name,
        String description,
        Double weight,
        Integer sortOrder
) {
}

