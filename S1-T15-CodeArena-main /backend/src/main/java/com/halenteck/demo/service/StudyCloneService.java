package com.halenteck.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.entity.ArtifactEntity;
import com.halenteck.demo.entity.EligibilityApprovalMode;
import com.halenteck.demo.entity.StudyArtifactSelectionEntity;
import com.halenteck.demo.entity.StudyCollaboratorEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyRatingCriterionEntity;
import com.halenteck.demo.entity.StudyTaskArtifactEntity;
import com.halenteck.demo.entity.StudyTaskDefinitionEntity;
import com.halenteck.demo.entity.StudyTaskRatingCriterionEntity;
import com.halenteck.demo.entity.StudyTemplateEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.permission.StudyCollaboratorRole;
import com.halenteck.demo.repository.ArtifactRepository;
import com.halenteck.demo.repository.StudyArtifactSelectionRepository;
import com.halenteck.demo.repository.StudyCollaboratorRepository;
import com.halenteck.demo.repository.StudyRatingCriterionRepository;
import com.halenteck.demo.repository.StudyRepository;
import com.halenteck.demo.repository.StudyTaskDefinitionRepository;
import com.halenteck.demo.repository.StudyTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyCloneService {

    private final StudyRepository studyRepository;
    private final StudyArtifactSelectionRepository studyArtifactSelectionRepository;
    private final StudyRatingCriterionRepository studyRatingCriterionRepository;
    private final StudyTaskDefinitionRepository studyTaskDefinitionRepository;
    private final StudyCollaboratorRepository studyCollaboratorRepository;
    private final ArtifactRepository artifactRepository;
    private final StudyTemplateRepository studyTemplateRepository;
    private final StudyTemplateService studyTemplateService;
    private final ObjectMapper objectMapper;

    public StudyCloneService(StudyRepository studyRepository,
                             StudyArtifactSelectionRepository studyArtifactSelectionRepository,
                             StudyRatingCriterionRepository studyRatingCriterionRepository,
                             StudyTaskDefinitionRepository studyTaskDefinitionRepository,
                             StudyCollaboratorRepository studyCollaboratorRepository,
                             ArtifactRepository artifactRepository,
                             StudyTemplateRepository studyTemplateRepository,
                             StudyTemplateService studyTemplateService) {
        this.studyRepository = studyRepository;
        this.studyArtifactSelectionRepository = studyArtifactSelectionRepository;
        this.studyRatingCriterionRepository = studyRatingCriterionRepository;
        this.studyTaskDefinitionRepository = studyTaskDefinitionRepository;
        this.studyCollaboratorRepository = studyCollaboratorRepository;
        this.artifactRepository = artifactRepository;
        this.studyTemplateRepository = studyTemplateRepository;
        this.studyTemplateService = studyTemplateService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Transactional
    public StudyEntity cloneStudy(StudyEntity sourceStudy,
                                  UserEntity creator,
                                  String title,
                                  String description) {
        Map<String, Object> config = studyTemplateService.buildStudyTemplateConfig(sourceStudy);
        StudyEntity cloned = instantiateFromConfig(
                config,
                creator,
                title,
                description,
                null,
                sourceStudy.getId(),
                buildProvenance("Cloned from Study #" + sourceStudy.getId())
        );
        cloned.setClonedFromStudyId(sourceStudy.getId());
        return cloned;
    }

    @Transactional
    public StudyEntity createFromTemplate(StudyTemplateEntity template,
                                          UserEntity creator,
                                          String title,
                                          String description) {
        Map<String, Object> config = studyTemplateService.readConfig(template);
        StudyEntity created = instantiateFromConfig(
                config,
                creator,
                title,
                description,
                template,
                null,
                buildProvenance("Template: " + template.getName())
        );
        created.setSourceTemplateId(template.getId());
        created.setSourceTemplateName(template.getName());
        template.setLastUsedAt(LocalDateTime.now());
        studyTemplateRepository.save(template);
        return created;
    }

    private StudyEntity instantiateFromConfig(Map<String, Object> config,
                                              UserEntity creator,
                                              String titleOverride,
                                              String descriptionOverride,
                                              StudyTemplateEntity template,
                                              Long clonedFromStudyId,
                                              String provenanceNote) {
        Map<String, Object> studyMap = (Map<String, Object>) config.get("study");
        StudyEntity newStudy = new StudyEntity(
                titleOverride != null ? titleOverride : (String) studyMap.get("title"),
                descriptionOverride != null ? descriptionOverride : (String) studyMap.get("description"),
                studyMap.get("blinded") != null && Boolean.TRUE.equals(studyMap.get("blinded")),
                creator
        );
        newStudy.setStatus(com.halenteck.demo.entity.StudyStatus.DRAFT);
        newStudy.setHasUnpublishedChanges(true);
        newStudy.setAccessWindowStart(parseDateTime(studyMap.get("accessWindowStart")));
        newStudy.setAccessWindowEnd(parseDateTime(studyMap.get("accessWindowEnd")));
        newStudy.setEligibilityRulesJson((String) studyMap.get("eligibilityRulesJson"));
        if (studyMap.get("eligibilityApprovalMode") != null) {
            newStudy.setEligibilityApprovalMode(EligibilityApprovalMode.valueOf(studyMap.get("eligibilityApprovalMode").toString()));
        }
        newStudy.setProvenanceNote(provenanceNote);
        if (template != null) {
            newStudy.setSourceTemplateId(template.getId());
            newStudy.setSourceTemplateName(template.getName());
        }
        if (clonedFromStudyId != null) {
            newStudy.setClonedFromStudyId(clonedFromStudyId);
        }
        StudyEntity savedStudy = studyRepository.save(newStudy);
        studyCollaboratorRepository.save(new StudyCollaboratorEntity(savedStudy, creator, StudyCollaboratorRole.OWNER));

        Map<Long, String> artifactAliasById = new HashMap<>();
        List<Map<String, Object>> artifacts = (List<Map<String, Object>>) config.get("artifacts");
        if (artifacts != null) {
            for (Map<String, Object> artifactConfig : artifacts) {
                Long artifactId = ((Number) artifactConfig.get("artifactId")).longValue();
                ArtifactEntity artifact = artifactRepository.findById(artifactId)
                        .orElseThrow(() -> new IllegalArgumentException("Artifact " + artifactId + " not found."));
                String alias = (String) artifactConfig.get("alias");
                StudyArtifactSelectionEntity selection = new StudyArtifactSelectionEntity(savedStudy, artifact, alias);
                studyArtifactSelectionRepository.save(selection);
                artifactAliasById.put(artifactId, alias);
            }
        }

        Map<String, StudyRatingCriterionEntity> criterionByKey = new HashMap<>();
        List<Map<String, Object>> ratingCriteria = (List<Map<String, Object>>) config.get("ratingCriteria");
        if (ratingCriteria != null) {
            for (Map<String, Object> criterionConfig : ratingCriteria) {
                String key = (String) criterionConfig.get("key");
                StudyRatingCriterionEntity entity = new StudyRatingCriterionEntity(
                        savedStudy,
                        (String) criterionConfig.get("name"),
                        (String) criterionConfig.get("description"),
                        criterionConfig.get("weight") != null ? ((Number) criterionConfig.get("weight")).doubleValue() : 1.0,
                        criterionConfig.get("sortOrder") != null ? ((Number) criterionConfig.get("sortOrder")).intValue() : 0
                );
                StudyRatingCriterionEntity savedCriterion = studyRatingCriterionRepository.save(entity);
                criterionByKey.put(key, savedCriterion);
            }
        }

        List<Map<String, Object>> tasks = (List<Map<String, Object>>) config.get("tasks");
        if (tasks != null) {
            for (Map<String, Object> taskConfig : tasks) {
                StudyTaskDefinitionEntity definition = new StudyTaskDefinitionEntity(
                        savedStudy,
                        (String) taskConfig.getOrDefault("instructions", ""),
                        taskConfig.get("sortOrder") != null ? ((Number) taskConfig.get("sortOrder")).intValue() : 0
                );
                studyTaskDefinitionRepository.save(definition);

                List<Long> artifactIds = ((List<?>) taskConfig.getOrDefault("artifactIds", List.of()))
                        .stream()
                        .map(value -> ((Number) value).longValue())
                        .toList();
                int position = 0;
                for (Long artifactId : artifactIds) {
                    ArtifactEntity artifact = artifactRepository.findById(artifactId)
                            .orElseThrow(() -> new IllegalArgumentException("Artifact " + artifactId + " not found."));
                    definition.getArtifacts().add(new StudyTaskArtifactEntity(definition, artifact, position++));
                }

                List<String> criterionKeys = (List<String>) taskConfig.getOrDefault("ratingCriteria", List.of());
                for (String key : criterionKeys) {
                    StudyRatingCriterionEntity criterion = criterionByKey.get(key);
                    if (criterion != null) {
                        definition.getRatingCriteria().add(new StudyTaskRatingCriterionEntity(definition, criterion));
                    }
                }
                studyTaskDefinitionRepository.save(definition);
            }
        }

        return savedStudy;
    }

    private String buildProvenance(String note) {
        return StringUtils.hasText(note) ? note : null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return LocalDateTime.parse(value.toString());
    }
}

