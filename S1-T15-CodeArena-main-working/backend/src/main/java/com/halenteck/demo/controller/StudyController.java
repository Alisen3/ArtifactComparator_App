package com.halenteck.demo.controller;

import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.dto.*;
import com.halenteck.demo.entity.*;
import com.halenteck.demo.permission.StudyCollaboratorRole;
import com.halenteck.demo.permission.StudyPermissionAction;
import com.halenteck.demo.repository.*;
import com.halenteck.demo.service.QuizService;
import com.halenteck.demo.service.StudyAuditService;
import com.halenteck.demo.service.StudyPermissionService;
import com.halenteck.demo.service.StudyPublishException;
import com.halenteck.demo.service.StudyPublishingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/studies")
public class StudyController {

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final ArtifactRepository artifactRepository;
    private final ComparisonTaskRepository taskRepository;
    private final QuizRepository quizRepository;
    private final QuizService quizService;
    private final StudyCollaboratorRepository collaboratorRepository;
    private final StudyPermissionService studyPermissionService;
    private final StudyAuditService studyAuditService;
    private final StudyVersionRepository studyVersionRepository;
    private final StudyPublishingService studyPublishingService;

    public StudyController(StudyRepository studyRepository,
                           UserRepository userRepository,
                           ArtifactRepository artifactRepository,
                           ComparisonTaskRepository taskRepository,
                           QuizRepository quizRepository,
                           QuizService quizService,
                           StudyCollaboratorRepository collaboratorRepository,
                           StudyPermissionService studyPermissionService,
                           StudyAuditService studyAuditService,
                           StudyVersionRepository studyVersionRepository,
                           StudyPublishingService studyPublishingService) {
        this.studyRepository = studyRepository;
        this.userRepository = userRepository;
        this.artifactRepository = artifactRepository;
        this.taskRepository = taskRepository;
        this.quizRepository = quizRepository;
        this.quizService = quizService;
        this.collaboratorRepository = collaboratorRepository;
        this.studyPermissionService = studyPermissionService;
        this.studyAuditService = studyAuditService;
        this.studyVersionRepository = studyVersionRepository;
        this.studyPublishingService = studyPublishingService;
    }

    /**
     * Endpoint 1: Yeni Çalışma Oluştur
     */
    @PostMapping
    public ResponseEntity<?> createStudy(@RequestBody CreateStudyRequest request, Principal principal) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(request.title())) {
            errors.add("Title is required.");
        }
        if (!StringUtils.hasText(request.description())) {
            errors.add("Description is required.");
        }
        if (request.accessWindowStart() != null && request.accessWindowEnd() != null &&
                !request.accessWindowEnd().isAfter(request.accessWindowStart())) {
            errors.add("Access window end must be after start.");
        }
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", errors));
        }

        UserEntity creator = getCurrentUser(principal);
        boolean isBlinded = request.blinded() != null && request.blinded();

        StudyEntity newStudy = new StudyEntity(request.title(), request.description(), isBlinded, creator);
        newStudy.setAccessWindowStart(request.accessWindowStart());
        newStudy.setAccessWindowEnd(request.accessWindowEnd());
        newStudy.setHasUnpublishedChanges(true);

        StudyEntity savedStudy = studyRepository.save(newStudy);

        collaboratorRepository.save(new StudyCollaboratorEntity(savedStudy, creator, StudyCollaboratorRole.OWNER));
        studyAuditService.record(savedStudy, creator, StudyAuditAction.STUDY_CREATED, Map.of(
                "title", savedStudy.getTitle(),
                "blinded", savedStudy.isBlinded()
        ));
        studyAuditService.record(savedStudy, creator, StudyAuditAction.COLLABORATOR_ADDED, Map.of(
                "collaboratorId", creator.getId(),
                "collaboratorName", creator.getName(),
                "role", StudyCollaboratorRole.OWNER.name()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(savedStudy);
    }

    /**
     * Endpoint 2: Çalışmaya Görev Ata
     * (GÜNCELLENDİ: Sonsuz döngüyü önlemek için DTO kullanır)
     */
    @PostMapping("/{studyId}/tasks")
    public ResponseEntity<AssignedTaskDTO> createComparisonTask(@PathVariable Long studyId,
                                                                @RequestBody CreateTaskRequest request,
                                                                Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);

        if (study.getStatus() == StudyStatus.CLOSED) {
            throw new IllegalStateException("Closed studies cannot accept new tasks.");
        }

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.MANAGE_TASKS,
                "You do not have permission to assign tasks for this study."
        );

        UserEntity participant = userRepository.findById(request.participantId())
                .orElseThrow(() -> new RuntimeException("Participant not found"));
        ArtifactEntity artifactA = artifactRepository.findById(request.artifactAId())
                .orElseThrow(() -> new RuntimeException("Artifact A not found"));
        ArtifactEntity artifactB = artifactRepository.findById(request.artifactBId())
                .orElseThrow(() -> new RuntimeException("Artifact B not found"));

        ComparisonTaskEntity newTask = new ComparisonTaskEntity(study, participant, artifactA, artifactB);
        if (study.getLatestPublishedVersionNumber() != null) {
            studyVersionRepository.findByStudyAndVersionNumber(study, study.getLatestPublishedVersionNumber())
                    .ifPresent(newTask::setStudyVersion);
        }
        taskRepository.save(newTask);

        return ResponseEntity.status(HttpStatus.CREATED).body(convertTask(newTask));
    }

    /**
     * Endpoint 3: Araştırmacının Çalışmalarını Listele
     */
    @GetMapping("/my-studies")
    public ResponseEntity<List<StudySummaryDTO>> getMyStudies(Principal principal) {
        UserEntity user = getCurrentUser(principal);
        List<StudySummaryDTO> results = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        List<StudyEntity> owned = studyRepository.findByCreator(user);
        for (StudyEntity study : owned) {
            StudyPermissionDTO permissions = studyPermissionService.describePermissions(study, user);
            results.add(convertToStudySummaryDTO(study, permissions));
            seen.add(study.getId());
        }

        collaboratorRepository.findAllByCollaborator(user).forEach(collab -> {
            StudyEntity study = collab.getStudy();
            if (seen.add(study.getId())) {
                StudyPermissionDTO permissions = studyPermissionService.describePermissions(study, user);
                results.add(convertToStudySummaryDTO(study, permissions));
            }
        });

        results.sort(Comparator.comparing(StudySummaryDTO::id));
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{studyId}")
    public ResponseEntity<?> updateStudy(@PathVariable Long studyId,
                                         @RequestBody UpdateStudyRequest request,
                                         Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to update this study."
        );

        if (study.getStatus() == StudyStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Closed studies cannot be edited."));
        }

        if (request.accessWindowStart() != null && request.accessWindowEnd() != null &&
                !request.accessWindowEnd().isAfter(request.accessWindowStart())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Access window end must be after start."));
        }

        boolean changed = false;
        if (StringUtils.hasText(request.title()) && !request.title().equals(study.getTitle())) {
            study.setTitle(request.title());
            changed = true;
        }
        if (StringUtils.hasText(request.description()) && !request.description().equals(study.getDescription())) {
            study.setDescription(request.description());
            changed = true;
        }
        if (request.blinded() != null && request.blinded() != study.isBlinded()) {
            study.setBlinded(request.blinded());
            changed = true;
        }
        if (request.accessWindowStart() != null) {
            study.setAccessWindowStart(request.accessWindowStart());
            changed = true;
        }
        if (request.accessWindowEnd() != null) {
            study.setAccessWindowEnd(request.accessWindowEnd());
            changed = true;
        }

        if (changed) {
            study.setHasUnpublishedChanges(true);
        }

        StudyEntity saved = studyRepository.save(study);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{studyId}/publish")
    public ResponseEntity<?> publishStudy(@PathVariable Long studyId, Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.PUBLISH,
                "You do not have permission to publish this study."
        );

        try {
            StudyVersionEntity version = studyPublishingService.publish(study, actor);
            studyRepository.save(study);
            PublishStudyResponse response = new PublishStudyResponse(
                    study.getId(),
                    version.getVersionNumber(),
                    version.getPublishedAt(),
                    "Study published successfully."
            );
            return ResponseEntity.ok(response);
        } catch (StudyPublishException ex) {
            return ResponseEntity.badRequest().body(Map.of("errors", ex.getErrors()));
        }
    }

    /**
     * Endpoint 4: Çalışmaya Quiz Ata
     */
    @PostMapping("/{studyId}/assign-quiz")
    public ResponseEntity<?> assignQuizToStudy(
            @PathVariable Long studyId,
            @RequestBody AssignQuizRequest request,
            Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        QuizEntity quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to assign quizzes to this study."
        );

        if (!quiz.getCreator().getId().equals(actor.getId())) {
            throw new AccessDeniedException("You are not the creator of this quiz.");
        }

        study.setCompetencyQuiz(quiz);
        study.setHasUnpublishedChanges(true);
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
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view tasks for this study."
        );

        List<ComparisonTaskEntity> tasks = taskRepository.findByStudy(study);
        List<AssignedTaskDTO> taskDTOs = tasks.stream()
                .map(this::convertTask)
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskDTOs);
    }

    @GetMapping("/{studyId}/permissions")
    public ResponseEntity<StudyPermissionDTO> getPermissions(@PathVariable Long studyId, Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        StudyPermissionDTO permissions = studyPermissionService.describePermissions(study, actor);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{studyId}/collaborators")
    public ResponseEntity<List<StudyCollaboratorDTO>> getCollaborators(@PathVariable Long studyId,
                                                                      Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view collaborators."
        );

        studyPermissionService.ensureCreatorIsTracked(study);
        List<StudyCollaboratorDTO> collaborators = collaboratorRepository.findAllByStudy(study)
                .stream()
                .map(this::convertCollaborator)
                .collect(Collectors.toList());
        return ResponseEntity.ok(collaborators);
    }

    @PostMapping("/{studyId}/collaborators")
    public ResponseEntity<StudyCollaboratorDTO> addCollaborator(@PathVariable Long studyId,
                                                                @RequestBody AddCollaboratorRequest request,
                                                                Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.INVITE,
                "You do not have permission to manage collaborators."
        );

        StudyCollaboratorRole requestedRole = request.role() != null
                ? request.role()
                : StudyCollaboratorRole.VIEWER;

        StudyCollaboratorRole actorRole = studyPermissionService.resolveRole(study, actor);
        if (!studyPermissionService.canAssignRole(actorRole, requestedRole)) {
            throw new AccessDeniedException("You cannot assign the requested role.");
        }

        UserEntity collaborator = resolveCollaboratorTarget(request);
        if (collaborator.getId().equals(study.getCreator().getId())) {
            throw new IllegalStateException("The primary owner is already part of this study.");
        }

        collaboratorRepository.findByStudyAndCollaborator(study, collaborator)
                .ifPresent(existing -> {
                    throw new IllegalStateException("This collaborator is already added.");
                });

        StudyCollaboratorEntity saved = collaboratorRepository.save(
                new StudyCollaboratorEntity(study, collaborator, requestedRole)
        );

        studyAuditService.record(study, actor, StudyAuditAction.COLLABORATOR_ADDED, Map.of(
                "collaboratorId", collaborator.getId(),
                "collaboratorName", collaborator.getName(),
                "role", requestedRole.name()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(convertCollaborator(saved));
    }

    @PatchMapping("/{studyId}/collaborators/{collaboratorId}")
    public ResponseEntity<StudyCollaboratorDTO> changeCollaboratorRole(@PathVariable Long studyId,
                                                                       @PathVariable Long collaboratorId,
                                                                       @RequestBody ChangeCollaboratorRoleRequest request,
                                                                       Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        StudyCollaboratorEntity collaboratorEntity = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new RuntimeException("Collaborator not found"));

        if (!collaboratorEntity.getStudy().getId().equals(study.getId())) {
            throw new IllegalStateException("Collaborator does not belong to this study.");
        }

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.INVITE,
                "You do not have permission to manage collaborators."
        );

        StudyCollaboratorRole newRole = request.role();
        if (newRole == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        StudyCollaboratorRole actorRole = studyPermissionService.resolveRole(study, actor);
        if (!studyPermissionService.canAssignRole(actorRole, newRole)) {
            throw new AccessDeniedException("You cannot assign the requested role.");
        }

        studyPermissionService.ensureOwnerRetentionForRoleChange(study, collaboratorEntity, newRole);

        StudyCollaboratorRole oldRole = collaboratorEntity.getRole();
        collaboratorEntity.setRole(newRole);
        StudyCollaboratorEntity saved = collaboratorRepository.save(collaboratorEntity);

        studyAuditService.record(study, actor, StudyAuditAction.COLLABORATOR_ROLE_CHANGED, Map.of(
                "collaboratorId", collaboratorEntity.getCollaborator().getId(),
                "fromRole", oldRole.name(),
                "toRole", newRole.name()
        ));

        return ResponseEntity.ok(convertCollaborator(saved));
    }

    @DeleteMapping("/{studyId}/collaborators/{collaboratorId}")
    public ResponseEntity<?> removeCollaborator(@PathVariable Long studyId,
                                                @PathVariable Long collaboratorId,
                                                Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        StudyCollaboratorEntity collaboratorEntity = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new RuntimeException("Collaborator not found"));

        if (!collaboratorEntity.getStudy().getId().equals(study.getId())) {
            throw new IllegalStateException("Collaborator does not belong to this study.");
        }

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.INVITE,
                "You do not have permission to manage collaborators."
        );

        studyPermissionService.ensureNotRemovingLastOwner(study, collaboratorEntity);

        collaboratorRepository.delete(collaboratorEntity);

        studyAuditService.record(study, actor, StudyAuditAction.COLLABORATOR_REMOVED, Map.of(
                "collaboratorId", collaboratorEntity.getCollaborator().getId(),
                "role", collaboratorEntity.getRole().name()
        ));

        return ResponseEntity.ok(Map.of("message", "Collaborator removed."));
    }

    private UserEntity getCurrentUser(Principal principal) {
        return userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private StudyEntity getStudyOrThrow(Long studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("Study not found"));
    }

    private StudySummaryDTO convertToStudySummaryDTO(StudyEntity study, StudyPermissionDTO permissions) {
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
                study.isBlinded(),
                quizDTO,
                permissions.role(),
                permissions,
                study.getStatus(),
                study.getLatestPublishedVersionNumber(),
                study.getNextVersionNumber(),
                study.isHasUnpublishedChanges(),
                study.getAccessWindowStart(),
                study.getAccessWindowEnd()
        );
    }

    private AssignedTaskDTO convertTask(ComparisonTaskEntity task) {
        return new AssignedTaskDTO(
                task.getId(),
                task.getParticipant().getId(),
                task.getParticipant().getName(),
                task.getArtifactA().getId(),
                task.getArtifactA().getFileName(),
                task.getArtifactB().getId(),
                task.getArtifactB().getFileName(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getCompletedAt(),
                task.getStudyVersion() != null ? task.getStudyVersion().getVersionNumber() : null
        );
    }

    private StudyCollaboratorDTO convertCollaborator(StudyCollaboratorEntity entity) {
        return new StudyCollaboratorDTO(
                entity.getId(),
                entity.getCollaborator().getId(),
                entity.getCollaborator().getName(),
                entity.getCollaboratorEmail(),
                entity.getRole(),
                entity.getCreatedAt()
        );
    }

    private UserEntity resolveCollaboratorTarget(AddCollaboratorRequest request) {
        if (request.userId() != null) {
            return userRepository.findById(request.userId())
                    .orElseThrow(() -> new RuntimeException("User not found."));
        }
        if (request.email() != null && !request.email().isBlank()) {
            return userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("User not found for provided email."));
        }
        throw new IllegalArgumentException("Either userId or email is required.");
    }
}
