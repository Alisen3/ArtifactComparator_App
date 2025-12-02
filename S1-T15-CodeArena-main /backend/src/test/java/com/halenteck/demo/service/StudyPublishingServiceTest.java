package com.halenteck.demo.service;

import com.halenteck.demo.entity.ArtifactEntity;
import com.halenteck.demo.entity.StudyArtifactSelectionEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyRatingCriterionEntity;
import com.halenteck.demo.entity.StudyStatus;
import com.halenteck.demo.entity.StudyVersionEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.ComparisonTaskRepository;
import com.halenteck.demo.repository.StudyArtifactSelectionRepository;
import com.halenteck.demo.repository.StudyRatingCriterionRepository;
import com.halenteck.demo.repository.StudyTaskDefinitionRepository;
import com.halenteck.demo.repository.StudyVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyPublishingServiceTest {

    @Mock
    private ComparisonTaskRepository taskRepository;

    @Mock
    private StudyVersionRepository studyVersionRepository;

    @Mock
    private StudyArtifactSelectionRepository studyArtifactSelectionRepository;

    @Mock
    private StudyRatingCriterionRepository studyRatingCriterionRepository;

    @Mock
    private StudyTaskDefinitionRepository studyTaskDefinitionRepository;

    @Mock
    private StudyAuditService auditService;

    @InjectMocks
    private StudyPublishingService publishingService;

    private StudyEntity study;
    private UserEntity owner;

    @BeforeEach
    void setUp() {
        owner = new UserEntity();
        study = new StudyEntity("Title", "Description", false, owner);
        study.setAccessWindowStart(LocalDateTime.now().plusDays(1));
        study.setAccessWindowEnd(LocalDateTime.now().plusDays(2));
        study.setHasUnpublishedChanges(true);
    }

    @Test
    void publish_whenValid_createsVersionAndUpdatesStudy() {
        when(taskRepository.countByStudy(study)).thenReturn(1L);
        when(taskRepository.findByStudy(study)).thenReturn(List.of());
        when(taskRepository.findByStudyAndStudyVersionIsNull(study)).thenReturn(List.of());
        when(studyArtifactSelectionRepository.countByStudy(study)).thenReturn(1L);
        when(studyArtifactSelectionRepository.findByStudyOrderByCreatedAtAsc(study)).thenReturn(List.of(sampleSelection()));
        when(studyRatingCriterionRepository.countByStudy(study)).thenReturn(1L);
        when(studyRatingCriterionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study)).thenReturn(List.of(sampleCriterion()));
        when(studyTaskDefinitionRepository.countByStudy(study)).thenReturn(1L);
        when(studyTaskDefinitionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study)).thenReturn(List.of());
        when(studyVersionRepository.save(any(StudyVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudyVersionEntity version = publishingService.publish(study, owner);

        assertNotNull(version);
        assertEquals(1, version.getVersionNumber());
        assertEquals(StudyStatus.PUBLISHED, study.getStatus());
        assertFalse(study.isHasUnpublishedChanges());
        assertEquals(2, study.getNextVersionNumber());
        verify(studyVersionRepository).save(any(StudyVersionEntity.class));
        verify(auditService).record(eq(study), eq(owner), any(), any(Map.class));
    }

    @Test
    void publish_missingAccessWindow_throwsException() {
        study.setAccessWindowStart(null);
        when(taskRepository.countByStudy(study)).thenReturn(1L);
        when(studyArtifactSelectionRepository.countByStudy(study)).thenReturn(1L);
        when(studyRatingCriterionRepository.countByStudy(study)).thenReturn(1L);
        when(studyTaskDefinitionRepository.countByStudy(study)).thenReturn(1L);

        StudyPublishException exception = assertThrows(StudyPublishException.class,
                () -> publishingService.publish(study, owner));

        assertTrue(exception.getErrors().stream().anyMatch(msg -> msg.contains("Access window")));
        verify(studyVersionRepository, never()).save(any());
    }

    @Test
    void publish_missingTaskDefinitionsFails() {
        when(taskRepository.countByStudy(study)).thenReturn(0L);
        when(studyArtifactSelectionRepository.countByStudy(study)).thenReturn(1L);
        when(studyRatingCriterionRepository.countByStudy(study)).thenReturn(1L);
        when(studyTaskDefinitionRepository.countByStudy(study)).thenReturn(0L);

        StudyPublishException exception = assertThrows(StudyPublishException.class,
                () -> publishingService.publish(study, owner));

        assertTrue(exception.getErrors().stream().anyMatch(msg -> msg.contains("task definition")));
    }

    @Test
    void publish_missingRatingCriteriaFails() {
        when(taskRepository.countByStudy(study)).thenReturn(1L);
        when(studyArtifactSelectionRepository.countByStudy(study)).thenReturn(1L);
        when(studyRatingCriterionRepository.countByStudy(study)).thenReturn(0L);
        when(studyTaskDefinitionRepository.countByStudy(study)).thenReturn(1L);

        StudyPublishException exception = assertThrows(StudyPublishException.class,
                () -> publishingService.publish(study, owner));

        assertTrue(exception.getErrors().stream().anyMatch(msg -> msg.contains("rating criterion")));
    }

    @Test
    void describeReadiness_reportsErrors() {
        when(taskRepository.countByStudy(study)).thenReturn(0L);
        when(studyArtifactSelectionRepository.countByStudy(study)).thenReturn(0L);
        when(studyRatingCriterionRepository.countByStudy(study)).thenReturn(0L);
        when(studyTaskDefinitionRepository.countByStudy(study)).thenReturn(0L);

        var readiness = publishingService.describeReadiness(study);

        assertFalse(readiness.ready());
        assertTrue(readiness.errors().size() >= 3);
    }

    private StudyArtifactSelectionEntity sampleSelection() {
        ArtifactEntity artifact = new ArtifactEntity();
        artifact.setId(42L);
        artifact.setFileName("sample.txt");
        artifact.setMimeType("text/plain");
        artifact.setOwnerId(1L);
        StudyArtifactSelectionEntity selection = new StudyArtifactSelectionEntity(study, artifact, "Sample");
        return selection;
    }

    private StudyRatingCriterionEntity sampleCriterion() {
        return new StudyRatingCriterionEntity(study, "Clarity", "desc", 1.0, 1);
    }
}


