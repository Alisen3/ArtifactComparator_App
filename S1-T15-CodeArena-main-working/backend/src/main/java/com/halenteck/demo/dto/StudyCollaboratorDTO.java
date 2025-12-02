package com.halenteck.demo.dto;

import com.halenteck.demo.permission.StudyCollaboratorRole;

import java.time.LocalDateTime;

public record StudyCollaboratorDTO(
        Long id,
        Long userId,
        String name,
        String email,
        StudyCollaboratorRole role,
        LocalDateTime addedAt
) {
}


