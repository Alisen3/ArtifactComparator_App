package com.halenteck.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.entity.StudyAuditLogEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.StudyAuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StudyAuditService {

    private final StudyAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudyAuditService(StudyAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(StudyEntity study,
                       UserEntity actor,
                       StudyAuditAction action,
                       Map<String, Object> details) {
        try {
            String json = details == null || details.isEmpty()
                    ? null
                    : objectMapper.writeValueAsString(details);
            StudyAuditLogEntity entry = new StudyAuditLogEntity(study, actor, action, json);
            auditLogRepository.save(entry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit details", e);
        }
    }
}


