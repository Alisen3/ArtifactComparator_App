package com.halenteck.demo.dto;

import com.halenteck.demo.entity.StudyInviteDeliveryMethod;
import com.halenteck.demo.entity.StudyInviteStatus;

import java.time.LocalDateTime;

public record StudyInviteDTO(
        Long id,
        String email,
        Long invitedUserId,
        String invitedUserName,
        StudyInviteDeliveryMethod deliveryMethod,
        StudyInviteStatus status,
        String token,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime acceptedAt,
        String acceptedByName
) {
}

