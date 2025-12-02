package com.halenteck.demo.dto;

import com.halenteck.demo.permission.StudyCollaboratorRole;

public record AddCollaboratorRequest(
        Long userId,
        String email,
        StudyCollaboratorRole role
) {
}


