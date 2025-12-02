package com.halenteck.demo.dto;

public record CreateStudyInviteRequest(
        String email,
        Integer expiresInHours,
        boolean shareableLink
) {
}

