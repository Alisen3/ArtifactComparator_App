package com.halenteck.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.entity.ComparisonTaskEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyStatus;
import com.halenteck.demo.entity.StudyVersionEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.ComparisonTaskRepository;
import com.halenteck.demo.repository.StudyVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudyPublishingService {

    private final ComparisonTaskRepository taskRepository;
    private final StudyVersionRepository versionRepository;
    private final StudyAuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudyPublishingService(ComparisonTaskRepository taskRepository,
                                  StudyVersionRepository versionRepository,
                                  StudyAuditService auditService) {
        this.taskRepository = taskRepository;
        this.versionRepository = versionRepository;
        this.auditService = auditService;
    }

    public StudyVersionEntity publish(StudyEntity study, UserEntity actor) {
        List<String> errors = validate(study);
        if (!errors.isEmpty()) {
            throw new StudyPublishException(errors);
        }

        int versionNumber = study.getNextVersionNumber();
        String snapshot = buildSnapshotJson(study);
        StudyVersionEntity version = new StudyVersionEntity(
                study,
                versionNumber,
                snapshot,
                LocalDateTime.now()
        );
        StudyVersionEntity savedVersion = versionRepository.save(version);

        List<ComparisonTaskEntity> tasksWithoutVersion = taskRepository.findByStudyAndStudyVersionIsNull(study);
        tasksWithoutVersion.forEach(task -> task.setStudyVersion(savedVersion));
        if (!tasksWithoutVersion.isEmpty()) {
            taskRepository.saveAll(tasksWithoutVersion);
        }

        study.setLatestPublishedVersionNumber(versionNumber);
        study.setNextVersionNumber(versionNumber + 1);
        study.setStatus(StudyStatus.PUBLISHED);
        study.setHasUnpublishedChanges(false);

        auditService.record(study, actor, StudyAuditAction.STUDY_PUBLISHED, Map.of(
                "versionNumber", versionNumber,
                "publishedAt", savedVersion.getPublishedAt()
        ));

        return savedVersion;
    }

    private List<String> validate(StudyEntity study) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(study.getTitle())) {
            errors.add("Title is required.");
        }
        if (!StringUtils.hasText(study.getDescription())) {
            errors.add("Description is required.");
        }
        if (study.getAccessWindowStart() == null || study.getAccessWindowEnd() == null) {
            errors.add("Access window start and end must be provided.");
        } else if (!study.getAccessWindowEnd().isAfter(study.getAccessWindowStart())) {
            errors.add("Access window end must be after start.");
        }
        if (taskRepository.countByStudy(study) == 0) {
            errors.add("At least one task must be configured before publishing.");
        }
        if (study.getStatus() == StudyStatus.CLOSED) {
            errors.add("Closed studies cannot be published.");
        }
        if (study.getStatus() == StudyStatus.PUBLISHED && !study.isHasUnpublishedChanges()) {
            errors.add("There are no changes to publish.");
        }
        return errors;
    }

    private String buildSnapshotJson(StudyEntity study) {
        Map<String, Object> payload = new HashMap<>();
        String windowStart = study.getAccessWindowStart() != null ? study.getAccessWindowStart().toString() : null;
        String windowEnd = study.getAccessWindowEnd() != null ? study.getAccessWindowEnd().toString() : null;

        payload.put("study", Map.of(
                "title", study.getTitle(),
                "description", study.getDescription(),
                "blinded", study.isBlinded(),
                "accessWindowStart", windowStart,
                "accessWindowEnd", windowEnd,
                "versionNumber", study.getNextVersionNumber()
        ));

        List<Map<String, Object>> tasks = taskRepository.findByStudy(study).stream()
                .map(task -> {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("taskId", task.getId());
                    taskMap.put("participantId", task.getParticipant().getId());
                    taskMap.put("artifactA", task.getArtifactA().getFileName());
                    taskMap.put("artifactB", task.getArtifactB().getFileName());
                    return taskMap;
                })
                .collect(Collectors.toList());
        payload.put("tasks", tasks);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build study snapshot", e);
        }
    }
}


