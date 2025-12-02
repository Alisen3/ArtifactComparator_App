package com.halenteck.demo.dto;

import com.halenteck.demo.entity.StudyEnrollmentStatus;

import java.time.LocalDateTime;

public record StudyEnrollmentRequestDTO(
        Long id,
        Long participantId,
        String participantName,
        String participantEmail,
        StudyEnrollmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String reviewerName,
        String reviewerNote
) {
}

