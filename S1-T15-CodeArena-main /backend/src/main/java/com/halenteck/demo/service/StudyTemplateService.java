package com.halenteck.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.entity.ArtifactEntity;
import com.halenteck.demo.entity.StudyArtifactSelectionEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyRatingCriterionEntity;
import com.halenteck.demo.entity.StudyTaskDefinitionEntity;
import com.halenteck.demo.entity.StudyTemplateEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.StudyArtifactSelectionRepository;
import com.halenteck.demo.repository.StudyRatingCriterionRepository;
import com.halenteck.demo.repository.StudyTaskDefinitionRepository;
import com.halenteck.demo.repository.StudyTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudyTemplateService {

    private final StudyTemplateRepository studyTemplateRepository;
    private final StudyArtifactSelectionRepository studyArtifactSelectionRepository;
    private final StudyRatingCriterionRepository studyRatingCriterionRepository;
    private final StudyTaskDefinitionRepository studyTaskDefinitionRepository;
    private final ObjectMapper objectMapper;

    public StudyTemplateService(StudyTemplateRepository studyTemplateRepository,
                                StudyArtifactSelectionRepository studyArtifactSelectionRepository,
                                StudyRatingCriterionRepository studyRatingCriterionRepository,
                                StudyTaskDefinitionRepository studyTaskDefinitionRepository) {
        this.studyTemplateRepository = studyTemplateRepository;
        this.studyArtifactSelectionRepository = studyArtifactSelectionRepository;
        this.studyRatingCriterionRepository = studyRatingCriterionRepository;
        this.studyTaskDefinitionRepository = studyTaskDefinitionRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public List<StudyTemplateEntity> listTemplates(UserEntity owner) {
        return studyTemplateRepository.findByOwnerOrderByUpdatedAtDesc(owner);
    }

    public StudyTemplateEntity createTemplate(StudyEntity study,
                                              UserEntity owner,
                                              String name,
                                              String description) {
        String templateName = StringUtils.hasText(name) ? name.trim() : study.getTitle() + " Template";
        String configJson = serializeConfig(buildStudyTemplateConfig(study));
        StudyTemplateEntity template = new StudyTemplateEntity(owner, templateName, description, configJson);
        return studyTemplateRepository.save(template);
    }

    public StudyTemplateEntity updateTemplate(StudyTemplateEntity template,
                                              String name,
                                              String description) {
        if (StringUtils.hasText(name)) {
            template.setName(name.trim());
        }
        if (description != null) {
            template.setDescription(description);
        }
        return studyTemplateRepository.save(template);
    }

    public void deleteTemplate(StudyTemplateEntity template) {
        studyTemplateRepository.delete(template);
    }

    public Map<String, Object> buildStudyTemplateConfig(StudyEntity study) {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> studyMap = new HashMap<>();
        studyMap.put("title", study.getTitle());
        studyMap.put("description", study.getDescription());
        studyMap.put("blinded", study.isBlinded());
        studyMap.put("accessWindowStart", study.getAccessWindowStart() != null ? study.getAccessWindowStart().toString() : null);
        studyMap.put("accessWindowEnd", study.getAccessWindowEnd() != null ? study.getAccessWindowEnd().toString() : null);
        studyMap.put("eligibilityRulesJson", study.getEligibilityRulesJson());
        studyMap.put("eligibilityApprovalMode", study.getEligibilityApprovalMode().name());
        config.put("study", studyMap);

        List<Map<String, Object>> artifacts = studyArtifactSelectionRepository.findByStudyOrderByCreatedAtAsc(study)
                .stream()
                .map(selection -> {
                    ArtifactEntity artifact = selection.getArtifact();
                    Map<String, Object> map = new HashMap<>();
                    map.put("artifactId", artifact.getId());
                    map.put("alias", selection.getAlias());
                    return map;
                })
                .collect(Collectors.toList());
        config.put("artifacts", artifacts);

        List<Map<String, Object>> criteria = studyRatingCriterionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study)
                .stream()
                .map(criterion -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("key", "criterion-" + criterion.getId());
                    map.put("name", criterion.getName());
                    map.put("description", criterion.getDescription());
                    map.put("weight", criterion.getWeight());
                    map.put("sortOrder", criterion.getSortOrder());
                    return map;
                })
                .collect(Collectors.toList());
        config.put("ratingCriteria", criteria);

        Map<Long, String> criterionKeyMap = criteria.stream()
                .collect(Collectors.toMap(
                        map -> Long.valueOf(((String) map.get("key")).split("-")[1]),
                        map -> (String) map.get("key")
                ));

        List<Map<String, Object>> tasks = studyTaskDefinitionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study)
                .stream()
                .map(task -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("instructions", task.getInstructions());
                    map.put("sortOrder", task.getSortOrder());
                    map.put("artifactIds", task.getArtifacts().stream()
                            .map(artifact -> artifact.getArtifact().getId())
                            .collect(Collectors.toList()));
                    map.put("ratingCriteria", task.getRatingCriteria().stream()
                            .map(rc -> criterionKeyMap.getOrDefault(rc.getRatingCriterion().getId(), rc.getRatingCriterion().getName()))
                            .collect(Collectors.toList()));
                    return map;
                })
                .collect(Collectors.toList());
        config.put("tasks", tasks);

        return config;
    }

    public String serializeConfig(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize template configuration", e);
        }
    }

    public Map<String, Object> readConfig(StudyTemplateEntity template) {
        try {
            return objectMapper.readValue(template.getConfigJson(), Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse template configuration", e);
        }
    }
}

