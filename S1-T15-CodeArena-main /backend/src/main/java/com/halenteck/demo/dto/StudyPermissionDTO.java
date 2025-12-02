package com.halenteck.demo.dto;

import com.halenteck.demo.permission.StudyCollaboratorRole;

public record StudyPermissionDTO(
        StudyCollaboratorRole role,
        boolean canView,
        boolean canEditDraft,
        boolean canInvite,
        boolean canPublish,
        boolean canClose,
        boolean canArchive,
        boolean canDelete,
        boolean canExport,
        boolean canManageTasks,
        boolean canViewAudit
) {
}


