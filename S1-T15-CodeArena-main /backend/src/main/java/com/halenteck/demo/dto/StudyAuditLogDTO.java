package com.halenteck.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.halenteck.demo.audit.StudyAuditAction;

import java.time.LocalDateTime;

public record StudyAuditLogDTO(
        Long id,
        StudyAuditAction action,
        Long actorId,
        String actorName,
        JsonNode details,
        LocalDateTime createdAt
) {
}

