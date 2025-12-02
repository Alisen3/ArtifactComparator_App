package com.halenteck.demo.service;

import com.halenteck.demo.UserRole;
import com.halenteck.demo.dto.StudyPermissionDTO;
import com.halenteck.demo.entity.StudyCollaboratorEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.permission.StudyCollaboratorRole;
import com.halenteck.demo.repository.StudyCollaboratorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudyPermissionServiceTest {

    @Mock
    private StudyCollaboratorRepository collaboratorRepository;

    @InjectMocks
    private StudyPermissionService studyPermissionService;

    private UserEntity owner;
    private StudyEntity study;

    @BeforeEach
    void setUp() throws Exception {
        owner = new UserEntity("Owner", "owner@example.com", "pw", UserRole.RESEARCHER);
        setId(owner, 1L);
        study = new StudyEntity("Title", "Desc", false, owner);
        when(collaboratorRepository.findByStudyAndCollaborator(eq(study), eq(owner)))
                .thenReturn(Optional.of(new StudyCollaboratorEntity(study, owner, StudyCollaboratorRole.OWNER)));
    }

    @Test
    void ownerHasAllPermissions() {
        StudyPermissionDTO dto = studyPermissionService.describePermissions(study, owner);
        assertEquals(StudyCollaboratorRole.OWNER, dto.role());
        assertTrue(dto.canPublish());
        assertTrue(dto.canInvite());
        assertTrue(dto.canExport());
    }

    @Test
    void editorCannotPublishOrDelete() throws Exception {
        UserEntity editor = new UserEntity("Editor", "editor@example.com", "pw", UserRole.RESEARCHER);
        setId(editor, 2L);
        StudyCollaboratorEntity collaboratorEntity = new StudyCollaboratorEntity(study, editor, StudyCollaboratorRole.EDITOR);

        when(collaboratorRepository.findByStudyAndCollaborator(eq(study), eq(editor)))
                .thenReturn(Optional.of(collaboratorEntity));

        StudyPermissionDTO dto = studyPermissionService.describePermissions(study, editor);

        assertFalse(dto.canPublish());
        assertFalse(dto.canDelete());
        assertTrue(dto.canInvite());
        assertTrue(dto.canManageTasks());
    }

    @Test
    void reviewerCanExportButCannotInvite() throws Exception {
        UserEntity reviewer = new UserEntity("Reviewer", "reviewer@example.com", "pw", UserRole.RESEARCHER);
        setId(reviewer, 3L);
        StudyCollaboratorEntity collaboratorEntity = new StudyCollaboratorEntity(study, reviewer, StudyCollaboratorRole.REVIEWER);

        when(collaboratorRepository.findByStudyAndCollaborator(eq(study), eq(reviewer)))
                .thenReturn(Optional.of(collaboratorEntity));

        StudyPermissionDTO dto = studyPermissionService.describePermissions(study, reviewer);

        assertTrue(dto.canExport());
        assertFalse(dto.canInvite());
        assertFalse(dto.canManageTasks());
    }

    @Test
    void viewerCannotExport() throws Exception {
        UserEntity viewer = new UserEntity("Viewer", "viewer@example.com", "pw", UserRole.RESEARCHER);
        setId(viewer, 4L);
        StudyCollaboratorEntity collaboratorEntity = new StudyCollaboratorEntity(study, viewer, StudyCollaboratorRole.VIEWER);

        when(collaboratorRepository.findByStudyAndCollaborator(eq(study), eq(viewer)))
                .thenReturn(Optional.of(collaboratorEntity));

        StudyPermissionDTO dto = studyPermissionService.describePermissions(study, viewer);
        assertFalse(dto.canExport());
        assertTrue(dto.canView());
    }

    @Test
    void editorCannotAssignOwnerRole() {
        assertFalse(studyPermissionService.canAssignRole(StudyCollaboratorRole.EDITOR, StudyCollaboratorRole.OWNER));
        assertTrue(studyPermissionService.canAssignRole(StudyCollaboratorRole.EDITOR, StudyCollaboratorRole.REVIEWER));
    }

    @Test
    void preventRemovingLastOwner() throws Exception {
        UserEntity otherOwner = new UserEntity("Owner2", "owner2@example.com", "pw", UserRole.RESEARCHER);
        setId(otherOwner, 5L);
        StudyCollaboratorEntity collaboratorEntity = new StudyCollaboratorEntity(study, otherOwner, StudyCollaboratorRole.OWNER);

        when(collaboratorRepository.findByStudyAndCollaborator(eq(study), eq(otherOwner)))
                .thenReturn(Optional.of(collaboratorEntity));
        when(collaboratorRepository.countByStudyAndRole(study, StudyCollaboratorRole.OWNER))
                .thenReturn(1L);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> studyPermissionService.ensureNotRemovingLastOwner(study, collaboratorEntity));
        assertTrue(exception.getMessage().contains("at least one owner"));
    }

    private void setId(UserEntity user, Long id) throws Exception {
        Field idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }
}


