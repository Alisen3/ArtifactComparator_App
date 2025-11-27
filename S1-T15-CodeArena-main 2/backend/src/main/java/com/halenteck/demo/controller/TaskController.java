package com.halenteck.demo.controller;

import com.halenteck.demo.dto.ArtifactSummaryDTO;
import com.halenteck.demo.dto.ComparisonTaskDetailDTO;
import com.halenteck.demo.dto.SubmitTaskRequest;
import com.halenteck.demo.dto.TaskResponseDTO;
import com.halenteck.demo.entity.ComparisonTaskEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.ComparisonTaskRepository;
import com.halenteck.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ComparisonTaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskController(ComparisonTaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponseDTO>> getMyTasks(Principal principal) {
        UserEntity participant = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<ComparisonTaskEntity> tasks = taskRepository.findByParticipant(participant);
        
        List<TaskResponseDTO> taskDTOs = tasks.stream()
                .map(task -> {
                    String uploaderAName = userRepository.findById(task.getArtifactA().getOwnerId())
                            .map(UserEntity::getName).orElse(null);
                    String uploaderBName = userRepository.findById(task.getArtifactB().getOwnerId())
                            .map(UserEntity::getName).orElse(null);
                    
                    return new TaskResponseDTO(
                            task.getId(),
                            task.getStudy().getId(),
                            task.getStudy().getTitle(),
                            task.getStudy().isBlinded(),
                            task.getStatus(),
                            task.getCreatedAt(),
                            // StoreArtifactEntity kullanıldığı için 'getFilename'
                            new ArtifactSummaryDTO(task.getArtifactA().getId(), task.getArtifactA().getFilename(), uploaderAName),
                            new ArtifactSummaryDTO(task.getArtifactB().getId(), task.getArtifactB().getFilename(), uploaderBName)
                    );
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(taskDTOs);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ComparisonTaskDetailDTO> getTaskDetails(@PathVariable Long taskId, Principal principal) {
        UserEntity participant = userRepository.findByName(principal.getName()).orElseThrow();
        ComparisonTaskEntity task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getParticipant().getId().equals(participant.getId())) {
            throw new AccessDeniedException("Not your task.");
        }
        return ResponseEntity.ok(new ComparisonTaskDetailDTO(task, userRepository));
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<ComparisonTaskDetailDTO> completeTask(@PathVariable Long taskId, @RequestBody SubmitTaskRequest request, Principal principal) {
        UserEntity participant = userRepository.findByName(principal.getName()).orElseThrow();
        ComparisonTaskEntity task = taskRepository.findById(taskId).orElseThrow();

        if (!task.getParticipant().getId().equals(participant.getId())) throw new AccessDeniedException("Not your task.");

        task.setAnnotations(request.annotations());
        task.setClarityA(request.clarityA()); task.setRelevanceA(request.relevanceA()); task.setAccuracyA(request.accuracyA()); task.setCommentA(request.commentA()); task.setHighlightDataA(request.highlightDataA());
        task.setClarityB(request.clarityB()); task.setRelevanceB(request.relevanceB()); task.setAccuracyB(request.accuracyB()); task.setCommentB(request.commentB()); task.setHighlightDataB(request.highlightDataB());
        
        task.setStatus(ComparisonTaskEntity.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);

       return ResponseEntity.ok(new ComparisonTaskDetailDTO(task, userRepository));
    }
}