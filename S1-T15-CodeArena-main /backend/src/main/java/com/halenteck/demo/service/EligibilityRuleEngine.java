package com.halenteck.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.dto.EligibilityConfigDTO;
import com.halenteck.demo.dto.EligibilityRuleDTO;
import com.halenteck.demo.dto.EligibilityRulesetDTO;
import com.halenteck.demo.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EligibilityRuleEngine {

    private final ObjectMapper objectMapper;

    public EligibilityRuleEngine() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public EligibilityConfigDTO parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return new EligibilityConfigDTO(null, List.of());
        }
        try {
            return objectMapper.readValue(json, EligibilityConfigDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid eligibility rules configuration", e);
        }
    }

    public Map<String, Object> evaluate(UserEntity candidate, EligibilityConfigDTO config) {
        Map<String, Object> result = new HashMap<>();
        if (config == null || config.rulesets() == null || config.rulesets().isEmpty()) {
            result.put("eligible", true);
            result.put("matchedRulesets", List.of());
            return result;
        }
        boolean overallEligible = true;
        for (EligibilityRulesetDTO ruleset : config.rulesets()) {
            boolean matches = evaluateRuleset(candidate, ruleset);
            if (!matches) {
                overallEligible = false;
                break;
            }
        }
        result.put("eligible", overallEligible);
        result.put("approvalMode", config.approvalMode());
        return result;
    }

    private boolean evaluateRuleset(UserEntity candidate, EligibilityRulesetDTO ruleset) {
        List<EligibilityRuleDTO> rules = ruleset.rules();
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        boolean isAnd = !"OR".equalsIgnoreCase(ruleset.logic());
        if (isAnd) {
            return rules.stream().allMatch(rule -> evaluateRule(candidate, rule));
        } else {
            return rules.stream().anyMatch(rule -> evaluateRule(candidate, rule));
        }
    }

    private boolean evaluateRule(UserEntity candidate, EligibilityRuleDTO rule) {
        String field = rule.field();
        String operator = rule.operator();
        String value = rule.value();
        if (field == null || operator == null) {
            return true;
        }
        return switch (field) {
            case "skills" -> evaluateSkills(candidate.getSkills(), operator, value);
            case "yearsOfExperience" -> evaluateNumeric(candidate.getYearsOfExperience(), operator, value);
            default -> true;
        };
    }

    private boolean evaluateSkills(String skills, String operator, String value) {
        if (skills == null || value == null) {
            return false;
        }
        String[] skillTokens = skills.toLowerCase().split(",");
        String needle = value.toLowerCase();
        return switch (operator) {
            case "contains" -> List.of(skillTokens).stream().anyMatch(token -> token.trim().equals(needle));
            case "not_contains" -> List.of(skillTokens).stream().noneMatch(token -> token.trim().equals(needle));
            default -> false;
        };
    }

    private boolean evaluateNumeric(Integer candidateValue, String operator, String value) {
        if (value == null) {
            return false;
        }
        int expected = Integer.parseInt(value);
        int actual = candidateValue != null ? candidateValue : 0;
        return switch (operator) {
            case ">=" -> actual >= expected;
            case "<=" -> actual <= expected;
            case ">" -> actual > expected;
            case "<" -> actual < expected;
            case "==" -> actual == expected;
            default -> false;
        };
    }
}

