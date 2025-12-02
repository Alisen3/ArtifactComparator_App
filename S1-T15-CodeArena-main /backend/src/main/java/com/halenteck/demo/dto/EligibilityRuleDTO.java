package com.halenteck.demo.dto;

import java.util.List;

public record EligibilityRuleDTO(
        String field,
        String operator,
        String value
) {
}

