package com.halenteck.demo.dto;

public record AcceptInviteResponse(
        Long studyId,
        String message,
        boolean pendingApproval
) {
}

