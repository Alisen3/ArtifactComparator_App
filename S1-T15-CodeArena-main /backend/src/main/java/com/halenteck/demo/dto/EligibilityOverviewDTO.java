package com.halenteck.demo.dto;

import java.util.List;
import java.util.Map;

public record EligibilityOverviewDTO(
        EligibilityConfigDTO config,
        Map<String, Integer> stats,
        List<StudyEnrollmentRequestDTO> pendingRequests
) {
}

