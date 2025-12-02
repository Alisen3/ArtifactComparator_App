// Dosya Yolu: demo/dto/AssignedTaskDTO.java
// YENİ DOSYA
package com.halenteck.demo.dto;

import com.halenteck.demo.entity.ComparisonTaskEntity;
import java.time.LocalDateTime;

/**
 * Bir Araştırmacıya (Researcher), bir çalışmaya (Study)
 * atamış olduğu görevleri (Tasks) listelemek için kullanılır.
 * (GET /api/studies/{studyId}/tasks)
 */
public record AssignedTaskDTO(
        Long taskId,
        Long participantId,
        String participantName,
        Long artifactAId,
        String artifactAFileName,
        Long artifactBId,
        String artifactBFileName,
        ComparisonTaskEntity.TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        Integer studyVersionNumber
) {
}