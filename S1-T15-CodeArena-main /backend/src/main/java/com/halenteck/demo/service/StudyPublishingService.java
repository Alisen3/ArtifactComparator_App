package com.halenteck.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.dto.PublishReadinessDTO;
import com.halenteck.demo.entity.ComparisonTaskEntity;
import com.halenteck.demo.entity.EligibilityApprovalMode;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyStatus;
import com.halenteck.demo.entity.StudyVersionEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.ComparisonTaskRepository;
import com.halenteck.demo.repository.StudyArtifactSelectionRepository;
import com.halenteck.demo.repository.StudyRatingCriterionRepository;
import com.halenteck.demo.repository.StudyTaskDefinitionRepository;
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
    private final StudyArtifactSelectionRepository studyArtifactSelectionRepository;
    private final StudyRatingCriterionRepository studyRatingCriterionRepository;
    private final StudyTaskDefinitionRepository studyTaskDefinitionRepository;
    private final StudyAuditService auditService;
    private final ObjectMapper objectMapper;

    public StudyPublishingService(ComparisonTaskRepository taskRepository,
                                  StudyVersionRepository versionRepository,
                                  StudyArtifactSelectionRepository studyArtifactSelectionRepository,
                                  StudyRatingCriterionRepository studyRatingCriterionRepository,
                                  StudyTaskDefinitionRepository studyTaskDefinitionRepository,
                                  StudyAuditService auditService) {
        this.taskRepository = taskRepository;
        this.versionRepository = versionRepository;
        this.studyArtifactSelectionRepository = studyArtifactSelectionRepository;
        this.studyRatingCriterionRepository = studyRatingCriterionRepository;
        this.studyTaskDefinitionRepository = studyTaskDefinitionRepository;
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public StudyVersionEntity publish(StudyEntity study, UserEntity actor) {
        List<String> errors = collectValidationErrors(study);
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

    public PublishReadinessDTO describeReadiness(StudyEntity study) {
        List<String> errors = collectValidationErrors(study);
        return new PublishReadinessDTO(
                errors.isEmpty(),
                errors,
                study.getNextVersionNumber(),
                study.getLatestPublishedVersionNumber()
        );
    }

    private List<String> collectValidationErrors(StudyEntity study) {
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
        long assignedTasks = taskRepository.countByStudy(study);
        long configuredTasks = studyTaskDefinitionRepository.countByStudy(study);
        if (configuredTasks == 0 && assignedTasks == 0) {
            errors.add("Add at least one task definition before publishing.");
        }
        if (studyArtifactSelectionRepository.countByStudy(study) == 0) {
            errors.add("Select at least one artifact for this study before publishing.");
        }
        if (studyRatingCriterionRepository.countByStudy(study) == 0) {
            errors.add("Add at least one rating criterion before publishing.");
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
        EligibilityApprovalMode approvalMode = study.getEligibilityApprovalMode() != null
                ? study.getEligibilityApprovalMode()
                : EligibilityApprovalMode.AUTO;

        Map<String, Object> studyMap = new HashMap<>();
        studyMap.put("title", study.getTitle());
        studyMap.put("description", study.getDescription());
        studyMap.put("blinded", study.isBlinded());
        studyMap.put("accessWindowStart", windowStart);
        studyMap.put("accessWindowEnd", windowEnd);
        studyMap.put("versionNumber", study.getNextVersionNumber());
        if (study.getEligibilityRulesJson() != null) {
            studyMap.put("eligibilityRules", study.getEligibilityRulesJson());
        }
        studyMap.put("eligibilityApprovalMode", approvalMode.name());
        payload.put("study", studyMap);

        List<Map<String, Object>> artifacts = studyArtifactSelectionRepository.findByStudyOrderByCreatedAtAsc(study)
                .stream()
                .map(selection -> {
                    Map<String, Object> artifactMap = new HashMap<>();
                    artifactMap.put("artifactId", selection.getArtifact().getId());
                    artifactMap.put("fileName", selection.getArtifact().getFileName());
                    artifactMap.put("mimeType", selection.getArtifact().getMimeType());
                    artifactMap.put("alias", selection.getAlias());
                    artifactMap.put("addedAt", selection.getCreatedAt());
                    return artifactMap;
                })
                .collect(Collectors.toList());
        payload.put("artifacts", artifacts);

        List<Map<String, Object>> ratingCriteria = studyRatingCriterionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study)
                .stream()
                .map(criterion -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", criterion.getId());
                    map.put("name", criterion.getName());
                    map.put("description", criterion.getDescription());
                    map.put("weight", criterion.getWeight());
                    map.put("sortOrder", criterion.getSortOrder());
                    return map;
                })
                .collect(Collectors.toList());
        payload.put("ratingCriteria", ratingCriteria);

        List<Map<String, Object>> taskDefinitions = studyTaskDefinitionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study)
                .stream()
                .map(def -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", def.getId());
                    map.put("instructions", def.getInstructions());
                    map.put("sortOrder", def.getSortOrder());
                    map.put("artifacts", def.getArtifacts().stream()
                            .map(artifact -> Map.of(
                                    "artifactId", artifact.getArtifact().getId(),
                                    "alias", artifact.getArtifact().getFileName(),
                                    "position", artifact.getPosition()
                            ))
                            .collect(Collectors.toList()));
                    map.put("ratingCriteria", def.getRatingCriteria().stream()
                            .map(rc -> rc.getRatingCriterion().getId())
                            .collect(Collectors.toList()));
                    return map;
                })
                .collect(Collectors.toList());
        payload.put("taskDefinitions", taskDefinitions);

        List<Map<String, Object>> tasks = taskRepository.findByStudy(study).stream()
                .map(task -> {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("taskId", task.getId());
                    taskMap.put("participantId", task.getParticipant().getId());
                    taskMap.put("artifactAId", task.getArtifactA().getId());
                    taskMap.put("artifactAName", task.getArtifactA().getFileName());
                    taskMap.put("artifactBId", task.getArtifactB().getId());
                    taskMap.put("artifactBName", task.getArtifactB().getFileName());
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


