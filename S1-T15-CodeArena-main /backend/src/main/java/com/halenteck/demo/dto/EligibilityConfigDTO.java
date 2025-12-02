package com.halenteck.demo.dto;

import com.halenteck.demo.entity.EligibilityApprovalMode;

import java.util.List;

public record EligibilityConfigDTO(
        EligibilityApprovalMode approvalMode,
        List<EligibilityRulesetDTO> rulesets
) {
}

