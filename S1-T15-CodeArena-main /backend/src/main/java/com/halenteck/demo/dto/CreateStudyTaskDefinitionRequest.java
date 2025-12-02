package com.halenteck.demo.dto;

import java.util.List;

public record CreateStudyTaskDefinitionRequest(
        String instructions,
        List<Long> artifactIds,
        List<Long> ratingCriterionIds
) {
}

