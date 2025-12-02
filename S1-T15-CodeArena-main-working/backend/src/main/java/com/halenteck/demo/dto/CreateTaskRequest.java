package com.halenteck.demo.dto;

public record CreateTaskRequest(
        Long participantId,
        Long artifactAId,
        Long artifactBId
) {
}