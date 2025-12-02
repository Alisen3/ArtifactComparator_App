// File: backend/src/main/java/com/halenteck/demo/controller/TaskController.java
package com.halenteck.demo.controller;

import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.dto.ArtifactSummaryDTO;
import com.halenteck.demo.dto.ComparisonTaskDetailDTO;
import com.halenteck.demo.dto.SubmitTaskRequest;
import com.halenteck.demo.dto.TaskResponseDTO;
import com.halenteck.demo.entity.ComparisonTaskEntity;
import com.halenteck.demo.entity.StudyStatus;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.ComparisonTaskRepository;
import com.halenteck.demo.repository.UserRepository;
import com.halenteck.demo.service.StudyAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ComparisonTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final StudyAuditService studyAuditService;

    public TaskController(ComparisonTaskRepository taskRepository,
                          UserRepository userRepository,
                          StudyAuditService studyAuditService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.studyAuditService = studyAuditService;
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponseDTO>> getMyTasks(Principal principal) {
        UserEntity participant = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<ComparisonTaskEntity> tasks = taskRepository.findByParticipant(participant).stream()
                .filter(task -> task.getStudy().getStatus() == StudyStatus.PUBLISHED && task.getStudyVersion() != null)
                .collect(Collectors.toList());
        
        List<TaskResponseDTO> taskDTOs = tasks.stream()
                .map(task -> {
                    // Fetch uploader names
                    String uploaderAName = userRepository.findById(task.getArtifactA().getOwnerId())
                            .map(UserEntity::getName)
                            .orElse(null);
                    String uploaderBName = userRepository.findById(task.getArtifactB().getOwnerId())
                            .map(UserEntity::getName)
                            .orElse(null);
                    
                    return new TaskResponseDTO(
                            task.getId(),
                            task.getStudy().getId(),
                            task.getStudy().getTitle(),
                            task.getStudy().isBlinded(),
                            task.getStatus(),
                            task.getCreatedAt(),
                            // --- UPDATED: Pass uploader name here too ---
                            new ArtifactSummaryDTO(
                                    task.getArtifactA().getId(), 
                                    task.getArtifactA().getFileName(),
                                    uploaderAName
                            ),
                            new ArtifactSummaryDTO(
                                    task.getArtifactB().getId(), 
                                    task.getArtifactB().getFileName(),
                                    uploaderBName
                            )
                            // --------------------------------------------
                    );
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(taskDTOs);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ComparisonTaskDetailDTO> getTaskDetails(
            @PathVariable Long taskId,
            Principal principal) {

        UserEntity participant = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ComparisonTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getParticipant().getId().equals(participant.getId())) {
            throw new AccessDeniedException("This task is not assigned to you.");
        }

        return ResponseEntity.ok(new ComparisonTaskDetailDTO(task, userRepository));
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<ComparisonTaskDetailDTO> completeTask(
            @PathVariable Long taskId,
            @RequestBody SubmitTaskRequest request,
            Principal principal) {

        UserEntity participant = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ComparisonTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getParticipant().getId().equals(participant.getId())) {
            throw new AccessDeniedException("This task is not assigned to you.");
        }

        task.setAnnotations(request.annotations());
        
        task.setClarityA(request.clarityA());
        task.setRelevanceA(request.relevanceA());
        task.setAccuracyA(request.accuracyA());
        task.setCommentA(request.commentA());
        task.setHighlightDataA(request.highlightDataA());

        task.setClarityB(request.clarityB());
        task.setRelevanceB(request.relevanceB());
        task.setAccuracyB(request.accuracyB());
        task.setCommentB(request.commentB());
        task.setHighlightDataB(request.highlightDataB());

        task.setStatus(ComparisonTaskEntity.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        taskRepository.save(task);
        studyAuditService.record(task.getStudy(), participant, StudyAuditAction.TASK_COMPLETED, Map.of(
                "taskId", task.getId(),
                "studyVersion", task.getStudyVersion() != null ? task.getStudyVersion().getVersionNumber() : null
        ));

       return ResponseEntity.ok(new ComparisonTaskDetailDTO(task, userRepository));
    }
}
