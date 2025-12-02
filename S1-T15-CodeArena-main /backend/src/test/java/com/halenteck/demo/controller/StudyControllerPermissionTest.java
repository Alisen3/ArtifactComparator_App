package com.halenteck.demo.controller;

import com.halenteck.demo.UserRole;
import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyStatus;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.permission.StudyPermissionAction;
import com.halenteck.demo.repository.ArtifactRepository;
import com.halenteck.demo.repository.ComparisonTaskRepository;
import com.halenteck.demo.repository.QuizRepository;
import com.halenteck.demo.repository.StudyArtifactSelectionRepository;
import com.halenteck.demo.repository.StudyAuditLogRepository;
import com.halenteck.demo.repository.StudyCollaboratorRepository;
import com.halenteck.demo.repository.StudyRepository;
import com.halenteck.demo.repository.StudyRatingCriterionRepository;
import com.halenteck.demo.repository.StudyTaskDefinitionRepository;
import com.halenteck.demo.repository.StudyVersionRepository;
import com.halenteck.demo.repository.UserRepository;
import com.halenteck.demo.service.QuizService;
import com.halenteck.demo.service.StudyArchiveService;
import com.halenteck.demo.service.StudyAuditService;
import com.halenteck.demo.service.StudyCloneService;
import com.halenteck.demo.service.StudyEligibilityService;
import com.halenteck.demo.service.StudyInviteService;
import com.halenteck.demo.service.StudyPermissionService;
import com.halenteck.demo.service.StudyPublishingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyControllerPermissionTest {

    @Mock
    private StudyRepository studyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ArtifactRepository artifactRepository;
    @Mock
    private ComparisonTaskRepository comparisonTaskRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizService quizService;
    @Mock
    private StudyCollaboratorRepository collaboratorRepository;
    @Mock
    private StudyArtifactSelectionRepository studyArtifactSelectionRepository;
    @Mock
    private StudyRatingCriterionRepository studyRatingCriterionRepository;
    @Mock
    private StudyAuditLogRepository studyAuditLogRepository;
    @Mock
    private StudyTaskDefinitionRepository studyTaskDefinitionRepository;
    @Mock
    private StudyPermissionService studyPermissionService;
    @Mock
    private StudyAuditService studyAuditService;
    @Mock
    private StudyVersionRepository studyVersionRepository;
    @Mock
    private StudyPublishingService studyPublishingService;
    @Mock
    private StudyArchiveService studyArchiveService;
    @Mock
    private StudyCloneService studyCloneService;
    @Mock
    private StudyInviteService studyInviteService;
    @Mock
    private StudyEligibilityService studyEligibilityService;

    private StudyController controller;
    private UserEntity owner;
    private StudyEntity study;
    private final Principal principal = () -> "owner";

    @BeforeEach
    void setUp() throws Exception {
        controller = new StudyController(
                studyRepository,
                userRepository,
                artifactRepository,
                comparisonTaskRepository,
                quizRepository,
                quizService,
                collaboratorRepository,
                studyArtifactSelectionRepository,
                studyRatingCriterionRepository,
                studyAuditLogRepository,
                studyTaskDefinitionRepository,
                studyPermissionService,
                studyAuditService,
                studyInviteService,
                studyVersionRepository,
                studyPublishingService,
                studyArchiveService,
                studyCloneService,
                studyEligibilityService
        );

        owner = new UserEntity("Owner", "owner@example.com", "pw", UserRole.RESEARCHER);
        setId(owner, 10L);
        study = new StudyEntity("Test", "Desc", false, owner);
        setStudyId(study, 20L);
        study.setStatus(StudyStatus.PUBLISHED);

        when(userRepository.findByName("owner")).thenReturn(Optional.of(owner));
        when(studyRepository.findById(1L)).thenReturn(Optional.of(study));
        when(studyRepository.save(study)).thenReturn(study);
    }

    @Test
    void closeStudyRequiresClosePermission() {
        doThrow(new AccessDeniedException("forbidden"))
                .when(studyPermissionService)
                .requirePermission(eq(study), eq(owner), eq(StudyPermissionAction.CLOSE), anyString());

        assertThrows(AccessDeniedException.class, () -> controller.closeStudy(1L, principal));
    }

    @Test
    void closeStudyUpdatesStatusAndAudits() {
        ResponseEntity<?> response = controller.closeStudy(1L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<StudyEntity> studyCaptor = ArgumentCaptor.forClass(StudyEntity.class);
        verify(studyRepository).save(studyCaptor.capture());

        StudyEntity saved = studyCaptor.getValue();
        assertNotNull(saved);
        assertEquals(StudyStatus.CLOSED, saved.getStatus());
        verify(studyAuditService).record(eq(saved), eq(owner), eq(StudyAuditAction.STUDY_CLOSED), anyMap());

        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof Map);
        assertEquals("Study closed successfully.", ((Map<?, ?>) body).get("message"));
    }

    private void setId(UserEntity user, Long id) throws Exception {
        Field idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }

    private void setStudyId(StudyEntity target, Long id) throws Exception {
        Field idField = StudyEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}

