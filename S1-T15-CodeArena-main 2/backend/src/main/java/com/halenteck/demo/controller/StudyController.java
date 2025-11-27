package com.halenteck.demo.controller;

import com.halenteck.demo.dto.*;
import com.halenteck.demo.entity.*;
import com.halenteck.demo.repository.*;
import com.halenteck.demo.service.QuizService;
// --- Store Yapıları ---
import com.halenteck.demo.store.entity.StoreArtifactEntity;
import com.halenteck.demo.store.repository.StoreArtifactRepo;
// ---------------------
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/studies")
public class StudyController {

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final StoreArtifactRepo artifactRepository; // ARTIK StoreArtifactRepo
    private final ComparisonTaskRepository taskRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;

    public StudyController(StudyRepository studyRepository,
                           UserRepository userRepository,
                           StoreArtifactRepo artifactRepository,
                           ComparisonTaskRepository taskRepository,
                           QuizRepository quizRepository,
                           QuizService quizService) {
        this.studyRepository = studyRepository;
        this.userRepository = userRepository;
        this.artifactRepository = artifactRepository;
        this.taskRepository = taskRepository;
        this.quizRepository = quizRepository;
        this.quizService = quizService;
    }

    // ... (createStudy metodu AYNI) ...
    @PostMapping
    public ResponseEntity<StudyEntity> createStudy(@RequestBody CreateStudyRequest request, Principal principal) {
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        boolean isBlinded = request.blinded() != null ? request.blinded() : false;
        StudyEntity newStudy = new StudyEntity(request.title(), request.description(), isBlinded, creator);
        return ResponseEntity.status(HttpStatus.CREATED).body(studyRepository.save(newStudy));
    }

    // --- Task Atama (GÜNCELLENDİ) ---
    @PostMapping("/{studyId}/tasks")
    public ResponseEntity<AssignedTaskDTO> createComparisonTask(
            @PathVariable Long studyId,
            @RequestBody CreateTaskRequest request,
            Principal principal) {
        
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        StudyEntity study = studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("Study not found"));
        UserEntity participant = userRepository.findById(request.participantId())
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        // StoreArtifactRepo üzerinden çekiyoruz
        StoreArtifactEntity artifactA = artifactRepository.findById(request.artifactAId())
                .orElseThrow(() -> new RuntimeException("Artifact A not found"));
        StoreArtifactEntity artifactB = artifactRepository.findById(request.artifactBId())
                .orElseThrow(() -> new RuntimeException("Artifact B not found"));

        if (!study.getCreator().getId().equals(creator.getId())) {
            throw new AccessDeniedException("You are not the creator of this study.");
        }

        ComparisonTaskEntity newTask = new ComparisonTaskEntity(study, participant, artifactA, artifactB);
        taskRepository.save(newTask);

        AssignedTaskDTO responseDTO = new AssignedTaskDTO(
                newTask.getId(),
                newTask.getParticipant().getId(),
                newTask.getParticipant().getName(),
                newTask.getArtifactA().getId(),
                newTask.getArtifactA().getFilename(), // getFilename (küçük n)
                newTask.getArtifactB().getId(),
                newTask.getArtifactB().getFilename(),
                newTask.getStatus(),
                newTask.getCreatedAt(),
                newTask.getCompletedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    // ... (Diğer metodlar: getMyStudies, assignQuizToStudy, vb. AYNI KALSIN) ...
    // Sadece getTasksForStudy içinde getFilename() güncellemesi yapın:

    @GetMapping("/{studyId}/tasks")
    public ResponseEntity<List<AssignedTaskDTO>> getTasksForStudy(
            @PathVariable Long studyId,
            Principal principal) {
        // ... (Baştaki kontroller aynı) ...
        UserEntity creator = userRepository.findByName(principal.getName()).orElseThrow();
        StudyEntity study = studyRepository.findById(studyId).orElseThrow();
        if (!study.getCreator().getId().equals(creator.getId())) throw new AccessDeniedException("Unauthorized");

        List<ComparisonTaskEntity> tasks = taskRepository.findByStudy(study);

        List<AssignedTaskDTO> taskDTOs = tasks.stream()
                .map(task -> new AssignedTaskDTO(
                        task.getId(),
                        task.getParticipant().getId(),
                        task.getParticipant().getName(),
                        task.getArtifactA().getId(),
                        task.getArtifactA().getFilename(),
                        task.getArtifactB().getId(),
                        task.getArtifactB().getFilename(),
                        task.getStatus(),
                        task.getCreatedAt(),
                        task.getCompletedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskDTOs);
    }
    
    // ... (convertToStudySummaryDTO AYNI) ...
    private StudySummaryDTO convertToStudySummaryDTO(StudyEntity study) {
        QuizSummaryDTO quizDTO = null;
        if (study.getCompetencyQuiz() != null) {
            QuizEntity q = study.getCompetencyQuiz();
            quizDTO = new QuizSummaryDTO(q.getId(), q.getTitle(), q.getDescription(), q.getDurationInMinutes(), q.getCreatedAt(), q.getQuestions() != null ? q.getQuestions().size() : 0);
        }
        return new StudySummaryDTO(study.getId(), study.getTitle(), study.getDescription(), study.isBlinded(), quizDTO);
    }
    // ... (Diğer endpointler AYNI) ...
    @GetMapping("/my-studies")
    public ResponseEntity<List<StudySummaryDTO>> getMyStudies(Principal principal) {
        UserEntity creator = userRepository.findByName(principal.getName()).orElseThrow();
        List<StudyEntity> studies = studyRepository.findByCreator(creator);
        return ResponseEntity.ok(studies.stream().map(this::convertToStudySummaryDTO).collect(Collectors.toList()));
    }
    @PostMapping("/{studyId}/assign-quiz")
    public ResponseEntity<?> assignQuizToStudy(@PathVariable Long studyId, @RequestBody AssignQuizRequest request, Principal principal) {
        // ... (Aynı kodlar)
        UserEntity creator = userRepository.findByName(principal.getName()).orElseThrow();
        StudyEntity study = studyRepository.findById(studyId).orElseThrow();
        QuizEntity quiz = quizRepository.findById(request.quizId()).orElseThrow();
        if (!study.getCreator().getId().equals(creator.getId()) || !quiz.getCreator().getId().equals(creator.getId())) throw new AccessDeniedException("Unauthorized");
        study.setCompetencyQuiz(quiz);
        studyRepository.save(study);
        return ResponseEntity.ok(Map.of("message", "Assigned"));
    }
    @GetMapping("/{studyId}/quiz")
    public ResponseEntity<?> getQuizForStudy(@PathVariable Long studyId, Principal principal) {
        try { return ResponseEntity.ok(quizService.getQuizForParticipant(studyId, principal)); }
        catch (Exception e) { return ResponseEntity.status(409).body(Map.of("message", e.getMessage())); }
    }
    @PostMapping("/{studyId}/quiz/submit")
    public ResponseEntity<?> submitQuizForStudy(@PathVariable Long studyId, @RequestBody QuizSubmitRequest request, Principal principal) {
        return ResponseEntity.ok(quizService.submitQuiz(studyId, request, principal));
    }
    @GetMapping("/{studyId}/quiz/submissions")
    public ResponseEntity<?> getQuizSubmissionsForStudy(@PathVariable Long studyId, Principal principal) {
        return ResponseEntity.ok(quizService.getSubmissionsForStudy(studyId, principal));
    }
}