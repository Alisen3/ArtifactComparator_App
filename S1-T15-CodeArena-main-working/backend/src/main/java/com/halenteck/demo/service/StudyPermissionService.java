package com.halenteck.demo.service;

import com.halenteck.demo.UserRole;
import com.halenteck.demo.dto.StudyPermissionDTO;
import com.halenteck.demo.entity.StudyCollaboratorEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.permission.StudyCollaboratorRole;
import com.halenteck.demo.permission.StudyPermissionAction;
import com.halenteck.demo.repository.StudyCollaboratorRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

@Service
public class StudyPermissionService {

    private final StudyCollaboratorRepository collaboratorRepository;
    private final Map<StudyCollaboratorRole, EnumSet<StudyPermissionAction>> roleMatrix = new EnumMap<>(StudyCollaboratorRole.class);

    public StudyPermissionService(StudyCollaboratorRepository collaboratorRepository) {
        this.collaboratorRepository = collaboratorRepository;
        roleMatrix.put(StudyCollaboratorRole.OWNER, EnumSet.allOf(StudyPermissionAction.class));
        roleMatrix.put(StudyCollaboratorRole.EDITOR, EnumSet.of(
                StudyPermissionAction.VIEW,
                StudyPermissionAction.EDIT_DRAFT,
                StudyPermissionAction.INVITE,
                StudyPermissionAction.EXPORT,
                StudyPermissionAction.MANAGE_TASKS,
                StudyPermissionAction.VIEW_AUDIT
        ));
        roleMatrix.put(StudyCollaboratorRole.REVIEWER, EnumSet.of(
                StudyPermissionAction.VIEW,
                StudyPermissionAction.EXPORT,
                StudyPermissionAction.VIEW_AUDIT
        ));
        roleMatrix.put(StudyCollaboratorRole.VIEWER, EnumSet.of(
                StudyPermissionAction.VIEW
        ));
    }

    public StudyPermissionDTO describePermissions(StudyEntity study, UserEntity user) {
        ensureCreatorIsTracked(study);
        StudyCollaboratorRole role = resolveRole(study, user);
        if (role == null) {
            throw new AccessDeniedException("You do not have access to this study.");
        }
        EnumSet<StudyPermissionAction> actions = roleMatrix.getOrDefault(role, EnumSet.noneOf(StudyPermissionAction.class));
        return new StudyPermissionDTO(
                role,
                actions.contains(StudyPermissionAction.VIEW),
                actions.contains(StudyPermissionAction.EDIT_DRAFT),
                actions.contains(StudyPermissionAction.INVITE),
                actions.contains(StudyPermissionAction.PUBLISH),
                actions.contains(StudyPermissionAction.ARCHIVE),
                actions.contains(StudyPermissionAction.DELETE),
                actions.contains(StudyPermissionAction.EXPORT),
                actions.contains(StudyPermissionAction.MANAGE_TASKS),
                actions.contains(StudyPermissionAction.VIEW_AUDIT)
        );
    }

    public void requirePermission(StudyEntity study,
                                  UserEntity user,
                                  StudyPermissionAction action,
                                  String errorMessage) {
        if (!hasPermission(study, user, action)) {
            throw new AccessDeniedException(errorMessage);
        }
    }

    public boolean hasPermission(StudyEntity study,
                                 UserEntity user,
                                 StudyPermissionAction action) {
        ensureCreatorIsTracked(study);
        StudyCollaboratorRole role = resolveRole(study, user);
        if (role == null) {
            return false;
        }
        EnumSet<StudyPermissionAction> actions = roleMatrix.get(role);
        return actions != null && actions.contains(action);
    }

    public StudyCollaboratorRole resolveRole(StudyEntity study, UserEntity user) {
        if (user.getRole() == UserRole.ADMIN) {
            return StudyCollaboratorRole.OWNER;
        }
        if (study.getCreator() != null && study.getCreator().getId().equals(user.getId())) {
            return StudyCollaboratorRole.OWNER;
        }
        return collaboratorRepository.findByStudyAndCollaborator(study, user)
                .map(StudyCollaboratorEntity::getRole)
                .orElse(null);
    }

    public boolean canAssignRole(StudyCollaboratorRole actorRole, StudyCollaboratorRole desiredRole) {
        if (actorRole == StudyCollaboratorRole.OWNER) {
            return true;
        }
        if (actorRole == StudyCollaboratorRole.EDITOR) {
            return desiredRole == StudyCollaboratorRole.REVIEWER || desiredRole == StudyCollaboratorRole.VIEWER;
        }
        return false;
    }

    public void ensureNotRemovingLastOwner(StudyEntity study, StudyCollaboratorEntity collaboratorEntity) {
        ensureCreatorIsTracked(study);
        if (collaboratorEntity.getRole() != StudyCollaboratorRole.OWNER) {
            return;
        }
        if (study.getCreator() != null && study.getCreator().getId().equals(collaboratorEntity.getCollaborator().getId())) {
            throw new IllegalStateException("The primary study owner cannot be removed.");
        }
        long ownerCount = collaboratorRepository.countByStudyAndRole(study, StudyCollaboratorRole.OWNER);
        if (ownerCount <= 1) {
            throw new IllegalStateException("A study must always have at least one owner.");
        }
    }

    public void ensureOwnerRetentionForRoleChange(StudyEntity study,
                                                  StudyCollaboratorEntity collaboratorEntity,
                                                  StudyCollaboratorRole newRole) {
        if (collaboratorEntity.getRole() == StudyCollaboratorRole.OWNER
                && newRole != StudyCollaboratorRole.OWNER) {
            ensureNotRemovingLastOwner(study, collaboratorEntity);
        }
    }

    public void ensureCreatorIsTracked(StudyEntity study) {
        if (study.getCreator() == null) {
            return;
        }
        collaboratorRepository.findByStudyAndCollaborator(study, study.getCreator())
                .orElseGet(() -> collaboratorRepository.save(
                        new StudyCollaboratorEntity(study, study.getCreator(), StudyCollaboratorRole.OWNER)
                ));
    }
}


