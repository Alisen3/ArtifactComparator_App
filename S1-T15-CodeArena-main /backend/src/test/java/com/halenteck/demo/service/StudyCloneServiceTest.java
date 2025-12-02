package com.halenteck.demo.service;

import com.halenteck.demo.entity.ArtifactEntity;
import com.halenteck.demo.entity.StudyArtifactSelectionEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyTaskDefinitionEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.entity.StudyCollaboratorEntity;
import com.halenteck.demo.repository.ArtifactRepository;
import com.halenteck.demo.repository.StudyArtifactSelectionRepository;
import com.halenteck.demo.repository.StudyCollaboratorRepository;
import com.halenteck.demo.repository.StudyRatingCriterionRepository;
import com.halenteck.demo.repository.StudyRepository;
import com.halenteck.demo.repository.StudyTaskDefinitionRepository;
import com.halenteck.demo.repository.StudyTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyCloneServiceTest {

    @Mock
    private StudyRepository studyRepository;
    @Mock
    private StudyArtifactSelectionRepository studyArtifactSelectionRepository;
    @Mock
    private StudyRatingCriterionRepository studyRatingCriterionRepository;
    @Mock
    private StudyTaskDefinitionRepository studyTaskDefinitionRepository;
    @Mock
    private StudyCollaboratorRepository studyCollaboratorRepository;
    @Mock
    private ArtifactRepository artifactRepository;
    @Mock
    private StudyTemplateRepository studyTemplateRepository;
    @Mock
    private StudyTemplateService studyTemplateService;

    @InjectMocks
    private StudyCloneService studyCloneService;

    private StudyEntity sourceStudy;
    private UserEntity owner;
    private ArtifactEntity artifact;

    @BeforeEach
    void setUp() throws Exception {
        owner = new UserEntity("Owner", "owner@example.com", "pw", null);
        setId(owner, 5L);
        sourceStudy = new StudyEntity("Source", "Desc", false, owner);
        setStudyId(sourceStudy, 10L);

        artifact = new ArtifactEntity();
        artifact.setId(100L);
        artifact.setOwnerId(owner.getId());
        artifact.setFileName("artifact.txt");
        artifact.setMimeType("text/plain");

        when(studyRepository.save(any(StudyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studyTaskDefinitionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studyRatingCriterionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studyArtifactSelectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(artifactRepository.findById(artifact.getId())).thenReturn(Optional.of(artifact));
    }

    @Test
    void cloneStudyCopiesArtifactsAndTasks() {
        Map<String, Object> studyData = new HashMap<>();
        studyData.put("title", "Source");
        studyData.put("description", "Desc");
        studyData.put("blinded", false);
        studyData.put("accessWindowStart", null);
        studyData.put("accessWindowEnd", null);

        Map<String, Object> config = Map.of(
                "study", studyData,
                "artifacts", List.of(Map.of(
                        "artifactId", artifact.getId(),
                        "alias", "Alias A"
                )),
                "ratingCriteria", List.of(Map.of(
                        "key", "crit-1",
                        "name", "Clarity",
                        "description", "desc",
                        "weight", 1.0,
                        "sortOrder", 0
                )),
                "tasks", List.of(Map.of(
                        "instructions", "Review answers",
                        "sortOrder", 0,
                        "artifactIds", List.of(artifact.getId()),
                        "ratingCriteria", List.of("crit-1")
                ))
        );
        when(studyTemplateService.buildStudyTemplateConfig(sourceStudy)).thenReturn(config);

        StudyEntity cloned = studyCloneService.cloneStudy(sourceStudy, owner, "Clone Title", "Clone Desc");

        assertNotNull(cloned);
        assertEquals(sourceStudy.getId(), cloned.getClonedFromStudyId());
        verify(studyCollaboratorRepository).save(any(StudyCollaboratorEntity.class));
        verify(studyArtifactSelectionRepository, atLeastOnce()).save(any(StudyArtifactSelectionEntity.class));
        verify(studyRatingCriterionRepository, atLeastOnce()).save(any());
        verify(studyTaskDefinitionRepository, atLeastOnce()).save(any(StudyTaskDefinitionEntity.class));
    }

    private void setId(UserEntity user, Long id) throws Exception {
        Field field = UserEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }

    private void setStudyId(StudyEntity study, Long id) throws Exception {
        Field field = StudyEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(study, id);
    }
}

