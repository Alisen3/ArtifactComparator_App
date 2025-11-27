// Dosya Yolu: demo/controller/StudyController.java
// (POST /tasks İÇİN SONSUZ DÖNGÜ HATASI DÜZELTİLDİ)

package com.halenteck.demo.controller;

import com.halenteck.demo.dto.*; // Tüm DTO'ları import et
import com.halenteck.demo.entity.*; // Tüm Entity'leri import et
import com.halenteck.demo.repository.*; // Tüm Repository'leri import et
import com.halenteck.demo.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // Stream API için

@RestController
@RequestMapping("/api/studies")
public class StudyController {

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final ArtifactRepository artifactRepository;
    private final ComparisonTaskRepository taskRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;

    public StudyController(StudyRepository studyRepository,
                           UserRepository userRepository,
                           ArtifactRepository artifactRepository,
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

    /**
     * Endpoint 1: Yeni Çalışma Oluştur
     */
    @PostMapping
    public ResponseEntity<StudyEntity> createStudy(@RequestBody CreateStudyRequest request, Principal principal) {
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Handle null case for blinded
        boolean isBlinded = request.blinded() != null ? request.blinded() : false;

        StudyEntity newStudy = new StudyEntity(request.title(), request.description(), isBlinded, creator);
        StudyEntity savedStudy = studyRepository.save(newStudy);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedStudy);
    }

    /**
     * Endpoint 2: Çalışmaya Görev Ata
     * (GÜNCELLENDİ: Sonsuz döngüyü önlemek için DTO kullanır)
     */
    @PostMapping("/{studyId}/tasks")
    public ResponseEntity<AssignedTaskDTO> createComparisonTask( // DÖNÜŞ TİPİ DEĞİŞTİ
                                                                 @PathVariable Long studyId,
                                                                 @RequestBody CreateTaskRequest request,
                                                                 Principal principal) {
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        StudyEntity study = studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("Study not found"));
        UserEntity participant = userRepository.findById(request.participantId())
                .orElseThrow(() -> new RuntimeException("Participant not found"));
        ArtifactEntity artifactA = artifactRepository.findById(request.artifactAId())
                .orElseThrow(() -> new RuntimeException("Artifact A not found"));
        ArtifactEntity artifactB = artifactRepository.findById(request.artifactBId())
                .orElseThrow(() -> new RuntimeException("Artifact B not found"));

        if (!study.getCreator().getId().equals(creator.getId())) {
            throw new AccessDeniedException("You are not the creator of this study.");
        }

        ComparisonTaskEntity newTask = new ComparisonTaskEntity(study, participant, artifactA, artifactB);

        // Veritabanına kaydet (newTask nesnesi ID'sini ve createdAt zamanını alır)
        taskRepository.save(newTask);

        // HAM ENTITY YERİNE GÜVENLİ DTO'YU DÖNDÜR
        AssignedTaskDTO responseDTO = new AssignedTaskDTO(
                newTask.getId(),
                newTask.getParticipant().getId(),
                newTask.getParticipant().getName(),
                newTask.getArtifactA().getId(),
                newTask.getArtifactA().getFileName(),
                newTask.getArtifactB().getId(),
                newTask.getArtifactB().getFileName(),
                newTask.getStatus(),
                newTask.getCreatedAt(),
                newTask.getCompletedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    /**
     * Endpoint 3: Araştırmacının Çalışmalarını Listele
     */
    @GetMapping("/my-studies")
    public ResponseEntity<List<StudySummaryDTO>> getMyStudies(Principal principal) {
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<StudyEntity> studies = studyRepository.findByCreator(creator);

        List<StudySummaryDTO> studyDTOs = studies.stream()
                .map(this::convertToStudySummaryDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(studyDTOs);
    }

    /**
     * Endpoint 4: Çalışmaya Quiz Ata
     */
    @PostMapping("/{studyId}/assign-quiz")
    public ResponseEntity<?> assignQuizToStudy(
            @PathVariable Long studyId,
            @RequestBody AssignQuizRequest request,
            Principal principal) {
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        StudyEntity study = studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("Study not found"));
        QuizEntity quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!study.getCreator().getId().equals(creator.getId())) {
            throw new AccessDeniedException("You are not the creator of this study.");
        }
        if (!quiz.getCreator().getId().equals(creator.getId())) {
            throw new AccessDeniedException("You are not the creator of this quiz.");
        }

        study.setCompetencyQuiz(quiz);
        studyRepository.save(study);
        return ResponseEntity.ok(Map.of("message", "Quiz '" + quiz.getTitle() + "' assigned to study '" + study.getTitle() + "'"));
    }


    /**
     * Endpoint 5: Katılımcı için Quiz'i Getir
     */
    @GetMapping("/{studyId}/quiz")
    public ResponseEntity<?> getQuizForStudy(
            @PathVariable Long studyId,
            Principal principal) {

        try {
            QuizTakeDTO quizDTO = quizService.getQuizForParticipant(studyId, principal);
            return ResponseEntity.ok(quizDTO);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Endpoint 6: Katılımcı Quiz'i Gönderir (Submit)
     */
    @PostMapping("/{studyId}/quiz/submit")
    public ResponseEntity<?> submitQuizForStudy(
            @PathVariable Long studyId,
            @RequestBody QuizSubmitRequest request,
            Principal principal) {

        Map<String, Object> result = quizService.submitQuiz(studyId, request, principal);
        return ResponseEntity.ok(result);
    }


    /**
     * Endpoint 7: Araştırmacı Quiz Sonuçlarını (Submission) Görür
     */
    @GetMapping("/{studyId}/quiz/submissions")
    public ResponseEntity<List<SubmissionSummaryDTO>> getQuizSubmissionsForStudy(
            @PathVariable Long studyId,
            Principal principal) {

        List<SubmissionSummaryDTO> submissions = quizService.getSubmissionsForStudy(studyId, principal);
        return ResponseEntity.ok(submissions);
    }

    /**
     * Endpoint 8: Bir Çalışmaya Atanmış Görevleri Listele
     */
    @GetMapping("/{studyId}/tasks")
    public ResponseEntity<List<AssignedTaskDTO>> getTasksForStudy(
            @PathVariable Long studyId,
            Principal principal) {
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        StudyEntity study = studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("Study not found"));

        if (!study.getCreator().getId().equals(creator.getId())) {
            throw new AccessDeniedException("You are not the creator of this study.");
        }

        List<ComparisonTaskEntity> tasks = taskRepository.findByStudy(study);

        List<AssignedTaskDTO> taskDTOs = tasks.stream()
                .map(task -> new AssignedTaskDTO(
                        task.getId(),
                        task.getParticipant().getId(),
                        task.getParticipant().getName(),
                        task.getArtifactA().getId(),
                        task.getArtifactA().getFileName(),
                        task.getArtifactB().getId(),
                        task.getArtifactB().getFileName(),
                        task.getStatus(),
                        task.getCreatedAt(),
                        task.getCompletedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskDTOs);
    }

    // --- YARDIMCI METOD (my-studies için) ---
    private StudySummaryDTO convertToStudySummaryDTO(StudyEntity study) {
        QuizSummaryDTO quizDTO = null;
        QuizEntity quiz = study.getCompetencyQuiz();

        if (quiz != null) {
            quizDTO = new QuizSummaryDTO(
                    quiz.getId(),
                    quiz.getTitle(),
                    quiz.getDescription(),
                    quiz.getDurationInMinutes(),
                    quiz.getCreatedAt(),
                    quiz.getQuestions() != null ? quiz.getQuestions().size() : 0
            );
        }

        return new StudySummaryDTO(
                study.getId(),
                study.getTitle(),
                study.getDescription(),
                quizDTO
        );
    }
}
