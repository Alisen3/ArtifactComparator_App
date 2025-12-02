package com.halenteck.demo.dto;

import java.util.List;

public record EligibilityRulesetDTO(
        String logic,
        List<EligibilityRuleDTO> rules
) {
}

