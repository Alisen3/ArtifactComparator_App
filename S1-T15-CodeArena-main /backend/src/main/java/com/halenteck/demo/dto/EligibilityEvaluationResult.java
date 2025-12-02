package com.halenteck.demo.dto;

public record EligibilityEvaluationResult(
        boolean eligible,
        String reason
) {
}

