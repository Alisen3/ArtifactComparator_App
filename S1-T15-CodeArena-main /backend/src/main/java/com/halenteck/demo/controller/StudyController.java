package com.halenteck.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.UserRole;
import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.dto.*;
import com.halenteck.demo.entity.*;
import com.halenteck.demo.permission.StudyCollaboratorRole;
import com.halenteck.demo.permission.StudyPermissionAction;
import com.halenteck.demo.repository.*;
import com.halenteck.demo.service.QuizService;
import com.halenteck.demo.service.StudyArchiveService;
import com.halenteck.demo.service.StudyAuditService;
import com.halenteck.demo.service.StudyCloneService;
import com.halenteck.demo.service.StudyEligibilityService;
import com.halenteck.demo.service.StudyInviteService;
import com.halenteck.demo.service.StudyPermissionService;
import com.halenteck.demo.service.StudyPublishException;
import com.halenteck.demo.service.StudyPublishingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
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
    private final StudyArtifactSelectionRepository studyArtifactSelectionRepository;
    private final StudyRatingCriterionRepository studyRatingCriterionRepository;
    private final StudyAuditLogRepository studyAuditLogRepository;
    private final StudyTaskDefinitionRepository studyTaskDefinitionRepository;
    private final StudyPermissionService studyPermissionService;
    private final StudyAuditService studyAuditService;
    private final StudyInviteService studyInviteService;
    private final StudyVersionRepository studyVersionRepository;
    private final StudyPublishingService studyPublishingService;
    private final StudyArchiveService studyArchiveService;
    private final StudyCloneService studyCloneService;
    private final StudyEligibilityService studyEligibilityService;
    private final ObjectMapper versionObjectMapper;

    public StudyController(StudyRepository studyRepository,
                           UserRepository userRepository,
                           ArtifactRepository artifactRepository,
                           ComparisonTaskRepository taskRepository,
                           QuizRepository quizRepository,
                           QuizService quizService,
                           StudyCollaboratorRepository collaboratorRepository,
                           StudyArtifactSelectionRepository studyArtifactSelectionRepository,
                           StudyRatingCriterionRepository studyRatingCriterionRepository,
                           StudyAuditLogRepository studyAuditLogRepository,
                           StudyTaskDefinitionRepository studyTaskDefinitionRepository,
                           StudyPermissionService studyPermissionService,
                           StudyAuditService studyAuditService,
                           StudyInviteService studyInviteService,
                           StudyVersionRepository studyVersionRepository,
                           StudyPublishingService studyPublishingService,
                           StudyArchiveService studyArchiveService,
                           StudyCloneService studyCloneService,
                           StudyEligibilityService studyEligibilityService) {
        this.studyRepository = studyRepository;
        this.userRepository = userRepository;
        this.artifactRepository = artifactRepository;
        this.taskRepository = taskRepository;
        this.quizRepository = quizRepository;
        this.quizService = quizService;
        this.collaboratorRepository = collaboratorRepository;
        this.studyArtifactSelectionRepository = studyArtifactSelectionRepository;
        this.studyRatingCriterionRepository = studyRatingCriterionRepository;
        this.studyAuditLogRepository = studyAuditLogRepository;
        this.studyTaskDefinitionRepository = studyTaskDefinitionRepository;
        this.studyPermissionService = studyPermissionService;
        this.studyAuditService = studyAuditService;
        this.studyInviteService = studyInviteService;
        this.studyVersionRepository = studyVersionRepository;
        this.studyPublishingService = studyPublishingService;
        this.studyArchiveService = studyArchiveService;
        this.studyCloneService = studyCloneService;
        this.studyEligibilityService = studyEligibilityService;
        this.versionObjectMapper = new ObjectMapper();
        this.versionObjectMapper.findAndRegisterModules();
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
        studyAuditService.record(study, actor, StudyAuditAction.TASK_ASSIGNED, Map.of(
                "taskId", newTask.getId(),
                "participantId", participant.getId(),
                "artifactAId", artifactA.getId(),
                "artifactBId", artifactB.getId()
        ));
        if (study.getStatus() == StudyStatus.PUBLISHED) {
            markUnpublishedChanges(study);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(convertTask(newTask));
    }

    /**
     * Endpoint 3: Araştırmacının Çalışmalarını Listele
     * For ADMIN users, returns all studies in the system
     * For other users, returns studies they own or collaborate on
     */
    @GetMapping("/my-studies")
    public ResponseEntity<List<StudySummaryDTO>> getMyStudies(Principal principal) {
        UserEntity user = getCurrentUser(principal);
        List<StudySummaryDTO> results = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        // If user is ADMIN, return all studies
        if (user.getRole() == UserRole.ADMIN) {
            List<StudyEntity> allStudies = studyRepository.findAll();
            for (StudyEntity study : allStudies) {
                StudyPermissionDTO permissions = studyPermissionService.describePermissions(study, user);
                results.add(convertToStudySummaryDTO(study, permissions));
            }
            results.sort(Comparator.comparing(StudySummaryDTO::id));
            return ResponseEntity.ok(results);
        }

        // For non-admin users, return only their studies
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
        List<String> changedFields = new ArrayList<>();
        if (StringUtils.hasText(request.title()) && !request.title().equals(study.getTitle())) {
            study.setTitle(request.title());
            changed = true;
            changedFields.add("title");
        }
        if (StringUtils.hasText(request.description()) && !request.description().equals(study.getDescription())) {
            study.setDescription(request.description());
            changed = true;
            changedFields.add("description");
        }
        if (request.blinded() != null && request.blinded() != study.isBlinded()) {
            study.setBlinded(request.blinded());
            changed = true;
            changedFields.add("blinded");
        }
        if (request.accessWindowStart() != null) {
            study.setAccessWindowStart(request.accessWindowStart());
            changed = true;
            changedFields.add("accessWindowStart");
        }
        if (request.accessWindowEnd() != null) {
            study.setAccessWindowEnd(request.accessWindowEnd());
            changed = true;
            changedFields.add("accessWindowEnd");
        }

        if (changed) {
            study.setHasUnpublishedChanges(true);
        }

        StudyEntity saved = studyRepository.save(study);
        if (changed) {
            studyAuditService.record(saved, actor, StudyAuditAction.STUDY_UPDATED, Map.of(
                    "fields", changedFields
            ));
        }
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{studyId}/publish")
    public ResponseEntity<?> publishStudy(@PathVariable Long studyId, Principal principal) {
        System.out.println("publishStudy");
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);

        System.out.println("actor: " + actor);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.PUBLISH,
                "You do not have permission to publish this study."
        );

        try {
            System.out.println("publishStudy try");
            StudyVersionEntity version = studyPublishingService.publish(study, actor);
            System.out.println("version: " + version);
            studyRepository.save(study);
            System.out.println("study: " + study);
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

    @GetMapping("/{studyId}/publish-readiness")
    public ResponseEntity<PublishReadinessDTO> getPublishReadiness(@PathVariable Long studyId,
                                                                   Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to inspect this study."
        );
        PublishReadinessDTO readiness = studyPublishingService.describeReadiness(study);
        return ResponseEntity.ok(readiness);
    }

    @PostMapping("/{studyId}/close")
    public ResponseEntity<?> closeStudy(@PathVariable Long studyId, Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);

        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.CLOSE,
                "You do not have permission to close this study."
        );

        if (study.getStatus() == StudyStatus.CLOSED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Study is already closed."));
        }
        if (study.getStatus() != StudyStatus.PUBLISHED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Only published studies can be closed."));
        }

        study.setStatus(StudyStatus.CLOSED);
        study.setHasUnpublishedChanges(false);
        StudyEntity saved = studyRepository.save(study);

        studyAuditService.record(saved, actor, StudyAuditAction.STUDY_CLOSED, Map.of(
                "closedAt", LocalDateTime.now().toString(),
                "closedBy", actor.getId()
        ));

        return ResponseEntity.ok(Map.of(
                "message", "Study closed successfully.",
                "studyId", saved.getId()
        ));
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
        studyAuditService.record(study, actor, StudyAuditAction.QUIZ_ASSIGNED, Map.of(
                "quizId", quiz.getId(),
                "quizTitle", quiz.getTitle()
        ));
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

    @GetMapping("/{studyId}/selected-artifacts")
    public ResponseEntity<List<StudyArtifactDTO>> getSelectedArtifacts(@PathVariable Long studyId,
                                                                       Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view artifacts."
        );
        List<StudyArtifactDTO> artifacts = studyArtifactSelectionRepository.findByStudyOrderByCreatedAtAsc(study)
                .stream()
                .map(this::convertArtifactSelection)
                .collect(Collectors.toList());
        return ResponseEntity.ok(artifacts);
    }

    @PostMapping("/{studyId}/selected-artifacts")
    public ResponseEntity<StudyArtifactDTO> addSelectedArtifact(@PathVariable Long studyId,
                                                                @RequestBody AddStudyArtifactRequest request,
                                                                Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure artifacts."
        );
        if (request.artifactId() == null) {
            throw new IllegalArgumentException("artifactId is required.");
        }
        ArtifactEntity artifact = artifactRepository.findById(request.artifactId())
                .orElseThrow(() -> new RuntimeException("Artifact not found."));
        if (!artifact.getOwnerId().equals(actor.getId()) && actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("You can only select artifacts you uploaded.");
        }
        studyArtifactSelectionRepository.findByStudyAndArtifact(study, artifact)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Artifact already selected for this study.");
                });
        String alias = StringUtils.hasText(request.alias()) ? request.alias().trim() : artifact.getFileName();
        StudyArtifactSelectionEntity saved = studyArtifactSelectionRepository.save(
                new StudyArtifactSelectionEntity(study, artifact, alias)
        );
        markUnpublishedChanges(study);
        studyAuditService.record(study, actor, StudyAuditAction.ARTIFACT_LINKED, Map.of(
                "selectionId", saved.getId(),
                "artifactId", artifact.getId(),
                "alias", saved.getAlias()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(convertArtifactSelection(saved));
    }

    @DeleteMapping("/{studyId}/selected-artifacts/{selectionId}")
    public ResponseEntity<?> removeSelectedArtifact(@PathVariable Long studyId,
                                                    @PathVariable Long selectionId,
                                                    Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure artifacts."
        );
        StudyArtifactSelectionEntity selection = studyArtifactSelectionRepository.findByIdAndStudy(selectionId, study)
                .orElseThrow(() -> new RuntimeException("Selected artifact not found."));
        studyArtifactSelectionRepository.delete(selection);
        markUnpublishedChanges(study);
        studyAuditService.record(study, actor, StudyAuditAction.ARTIFACT_UNLINKED, Map.of(
                "selectionId", selection.getId(),
                "artifactId", selection.getArtifact().getId()
        ));
        return ResponseEntity.ok(Map.of("message", "Artifact removed from study."));
    }

    @GetMapping("/{studyId}/rating-criteria")
    public ResponseEntity<List<StudyRatingCriterionDTO>> getRatingCriteria(@PathVariable Long studyId,
                                                                           Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view rating criteria."
        );
        List<StudyRatingCriterionDTO> criteria = studyRatingCriterionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study)
                .stream()
                .map(this::convertCriterion)
                .collect(Collectors.toList());
        return ResponseEntity.ok(criteria);
    }

    @PostMapping("/{studyId}/rating-criteria")
    public ResponseEntity<StudyRatingCriterionDTO> addRatingCriterion(@PathVariable Long studyId,
                                                                      @RequestBody CreateStudyRatingCriterionRequest request,
                                                                      Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure rating criteria."
        );
        String name = request.name() != null ? request.name().trim() : "";
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Criterion name is required.");
        }
        double weight = request.weight() != null ? request.weight() : 1.0;
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive.");
        }
        int sortOrder = request.sortOrder() != null
                ? request.sortOrder()
                : (int) studyRatingCriterionRepository.countByStudy(study) + 1;
        StudyRatingCriterionEntity entity = new StudyRatingCriterionEntity(
                study,
                name,
                request.description(),
                weight,
                sortOrder
        );
        StudyRatingCriterionEntity saved = studyRatingCriterionRepository.save(entity);
        markUnpublishedChanges(study);
        studyAuditService.record(study, actor, StudyAuditAction.RATING_CRITERION_ADDED, Map.of(
                "criterionId", saved.getId(),
                "name", saved.getName(),
                "weight", saved.getWeight()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(convertCriterion(saved));
    }

    @PatchMapping("/{studyId}/rating-criteria/{criterionId}")
    public ResponseEntity<StudyRatingCriterionDTO> updateRatingCriterion(@PathVariable Long studyId,
                                                                         @PathVariable Long criterionId,
                                                                         @RequestBody UpdateStudyRatingCriterionRequest request,
                                                                         Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure rating criteria."
        );
        StudyRatingCriterionEntity entity = studyRatingCriterionRepository.findByIdAndStudy(criterionId, study)
                .orElseThrow(() -> new RuntimeException("Rating criterion not found."));

        if (request.name() != null) {
            if (!StringUtils.hasText(request.name().trim())) {
                throw new IllegalArgumentException("Criterion name cannot be blank.");
            }
            entity.setName(request.name().trim());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.weight() != null) {
            if (request.weight() <= 0) {
                throw new IllegalArgumentException("Weight must be positive.");
            }
            entity.setWeight(request.weight());
        }
        if (request.sortOrder() != null) {
            entity.setSortOrder(request.sortOrder());
        }
        StudyRatingCriterionEntity saved = studyRatingCriterionRepository.save(entity);
        markUnpublishedChanges(study);
        Map<String, Object> updates = new HashMap<>();
        if (request.name() != null) {
            updates.put("name", saved.getName());
        }
        if (request.description() != null) {
            updates.put("description", saved.getDescription());
        }
        if (request.weight() != null) {
            updates.put("weight", saved.getWeight());
        }
        if (request.sortOrder() != null) {
            updates.put("sortOrder", saved.getSortOrder());
        }
        studyAuditService.record(study, actor, StudyAuditAction.RATING_CRITERION_UPDATED,
                updates.isEmpty() ? Map.of("criterionId", saved.getId()) : enrichWithId(updates, saved.getId()));
        return ResponseEntity.ok(convertCriterion(saved));
    }

    @DeleteMapping("/{studyId}/rating-criteria/{criterionId}")
    public ResponseEntity<?> deleteRatingCriterion(@PathVariable Long studyId,
                                                   @PathVariable Long criterionId,
                                                   Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure rating criteria."
        );
        StudyRatingCriterionEntity entity = studyRatingCriterionRepository.findByIdAndStudy(criterionId, study)
                .orElseThrow(() -> new RuntimeException("Rating criterion not found."));
        studyRatingCriterionRepository.delete(entity);
        markUnpublishedChanges(study);
        studyAuditService.record(study, actor, StudyAuditAction.RATING_CRITERION_REMOVED, Map.of(
                "criterionId", entity.getId(),
                "name", entity.getName()
        ));
        return ResponseEntity.ok(Map.of("message", "Rating criterion removed."));
    }

    @GetMapping("/{studyId}/eligibility")
    public ResponseEntity<EligibilityOverviewDTO> getEligibility(@PathVariable Long studyId,
                                                                 Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view eligibility."
        );
        EligibilityOverviewDTO overview = studyEligibilityService.describeEligibility(study);
        return ResponseEntity.ok(overview);
    }

    @PutMapping("/{studyId}/eligibility")
    public ResponseEntity<EligibilityOverviewDTO> updateEligibility(@PathVariable Long studyId,
                                                                    @RequestBody UpdateEligibilityRequest request,
                                                                    Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to update eligibility."
        );
        if (request.config() == null) {
            throw new IllegalArgumentException("Config is required.");
        }
        studyEligibilityService.updateConfig(study, request.config());
        studyRepository.save(study);
        studyAuditService.record(study, actor, StudyAuditAction.STUDY_UPDATED, Map.of(
                "change", "ELIGIBILITY_UPDATED"
        ));
        EligibilityOverviewDTO overview = studyEligibilityService.describeEligibility(study);
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/{studyId}/enrollment-requests")
    public ResponseEntity<List<StudyEnrollmentRequestDTO>> listEnrollmentRequests(@PathVariable Long studyId,
                                                                                  Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.INVITE,
                "You do not have permission to review enrollments."
        );
        return ResponseEntity.ok(studyEligibilityService.listRequests(study));
    }

    @PostMapping("/{studyId}/enrollment-requests/{requestId}/approve")
    public ResponseEntity<?> approveEnrollment(@PathVariable Long studyId,
                                               @PathVariable Long requestId,
                                               Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.INVITE,
                "You do not have permission to approve enrollments."
        );
        StudyEnrollmentRequestEntity request = studyEligibilityService.getRequestOrThrow(requestId);
        if (!request.getStudy().getId().equals(study.getId())) {
            throw new IllegalArgumentException("Request does not belong to this study.");
        }
        studyEligibilityService.approveRequest(request, actor);
        studyAuditService.record(study, actor, StudyAuditAction.INVITE_ACCEPTED, Map.of(
                "requestId", requestId,
                "participantId", request.getParticipant().getId(),
                "status", "APPROVED_MANUAL"
        ));
        return ResponseEntity.ok(Map.of("message", "Enrollment approved."));
    }

    @PostMapping("/{studyId}/enrollment-requests/{requestId}/reject")
    public ResponseEntity<?> rejectEnrollment(@PathVariable Long studyId,
                                              @PathVariable Long requestId,
                                              @RequestBody(required = false) Map<String, String> payload,
                                              Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.INVITE,
                "You do not have permission to reject enrollments."
        );
        StudyEnrollmentRequestEntity request = studyEligibilityService.getRequestOrThrow(requestId);
        if (!request.getStudy().getId().equals(study.getId())) {
            throw new IllegalArgumentException("Request does not belong to this study.");
        }
        String note = payload != null ? payload.getOrDefault("note", null) : null;
        studyEligibilityService.rejectRequest(request, actor, note);
        studyAuditService.record(study, actor, StudyAuditAction.STUDY_UPDATED, Map.of(
                "requestId", requestId,
                "status", "REJECTED"
        ));
        return ResponseEntity.ok(Map.of("message", "Enrollment rejected."));
    }

    @GetMapping("/{studyId}/invites")
    public ResponseEntity<List<StudyInviteDTO>> getStudyInvites(@PathVariable Long studyId,
                                                                Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view invitations."
        );
        List<StudyInviteDTO> invites = studyInviteService.listInvites(study);
        return ResponseEntity.ok(invites);
    }

    @PostMapping("/{studyId}/invites")
    public ResponseEntity<StudyInviteDTO> createStudyInvite(@PathVariable Long studyId,
                                                            @RequestBody CreateStudyInviteRequest request,
                                                            Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.INVITE,
                "You do not have permission to invite participants."
        );
        String normalizedEmail = normalizeEmail(request.email());
        if (!request.shareableLink() && !StringUtils.hasText(normalizedEmail)) {
            throw new IllegalArgumentException("Email is required for email invitations.");
        }
        if (normalizedEmail != null) {
            ensureNotAlreadyParticipant(study, normalizedEmail);
            studyInviteService.ensureNoDuplicateInvite(study, normalizedEmail);
        }
        StudyInviteDeliveryMethod method = request.shareableLink()
                ? StudyInviteDeliveryMethod.LINK
                : StudyInviteDeliveryMethod.EMAIL;
        UserEntity invitedUser = null;
        if (normalizedEmail != null) {
            invitedUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        }
        StudyInviteEntity invite = studyInviteService.createInvite(
                study,
                actor,
                normalizedEmail,
                method,
                request.expiresInHours(),
                invitedUser
        );
        StudyInviteDTO dto = studyInviteService.toDto(invite);
        studyAuditService.record(study, actor, StudyAuditAction.INVITE_CREATED, Map.of(
                "inviteId", invite.getId(),
                "delivery", method.name(),
                "email", normalizedEmail
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<AcceptInviteResponse> acceptInvite(@PathVariable String token,
                                                             Principal principal) {
        UserEntity participant = getCurrentUser(principal);
        StudyInviteEntity invite = studyInviteService.getValidInvite(token);
        StudyEntity study = invite.getStudy();
        EligibilityEvaluationResult evaluation = studyEligibilityService.evaluateCandidate(study, participant);
        if (!evaluation.eligible()) {
            throw new IllegalStateException(evaluation.reason());
        }
        if (study.getEligibilityApprovalMode() == EligibilityApprovalMode.MANUAL) {
            studyInviteService.markUnderReview(invite, participant);
            studyEligibilityService.createPendingRequest(study, participant, invite, evaluation);
            studyAuditService.record(study, participant, StudyAuditAction.INVITE_ACCEPTED, Map.of(
                    "inviteId", invite.getId(),
                    "participantId", participant.getId(),
                    "status", "UNDER_REVIEW"
            ));
            return ResponseEntity.ok(new AcceptInviteResponse(
                    study.getId(),
                    "Your enrollment request is pending review.",
                    true
            ));
        } else {
            studyInviteService.finalizeAcceptance(invite, participant);
            studyRepository.save(study);
            studyAuditService.record(study, participant, StudyAuditAction.INVITE_ACCEPTED, Map.of(
                    "inviteId", invite.getId(),
                    "participantId", participant.getId(),
                    "status", "APPROVED"
            ));
            return ResponseEntity.ok(new AcceptInviteResponse(
                    study.getId(),
                    "You are now enrolled in " + study.getTitle(),
                    false
            ));
        }
    }

    @GetMapping("/{studyId}/task-definitions")
    public ResponseEntity<List<StudyTaskDefinitionDTO>> getTaskDefinitions(@PathVariable Long studyId,
                                                                           Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view tasks."
        );
        Map<Long, String> aliasMap = loadArtifactAliasMap(study);
        List<StudyTaskDefinitionDTO> definitions = studyTaskDefinitionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study)
                .stream()
                .map(def -> convertTaskDefinition(def, aliasMap))
                .collect(Collectors.toList());
        return ResponseEntity.ok(definitions);
    }

    @PostMapping("/{studyId}/task-definitions")
    public ResponseEntity<StudyTaskDefinitionDTO> createTaskDefinition(@PathVariable Long studyId,
                                                                       @RequestBody CreateStudyTaskDefinitionRequest request,
                                                                       Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure tasks."
        );
        List<ArtifactEntity> artifacts = resolveArtifactsForTask(study, request.artifactIds());
        List<StudyRatingCriterionEntity> criteria = resolveRatingCriteriaForTask(study, request.ratingCriterionIds());
        int nextSortOrder = (int) studyTaskDefinitionRepository.countByStudy(study);
        StudyTaskDefinitionEntity definition = new StudyTaskDefinitionEntity(
                study,
                request.instructions(),
                nextSortOrder
        );
        applyArtifactsAndCriteria(definition, artifacts, criteria);
        StudyTaskDefinitionEntity saved = studyTaskDefinitionRepository.save(definition);
        markUnpublishedChanges(study);
        studyAuditService.record(study, actor, StudyAuditAction.STUDY_UPDATED, Map.of(
                "taskDefinitionId", saved.getId(),
                "change", "TASK_CREATED"
        ));
        Map<Long, String> aliasMap = loadArtifactAliasMap(study);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertTaskDefinition(saved, aliasMap));
    }

    @PutMapping("/{studyId}/task-definitions/{taskId}")
    public ResponseEntity<StudyTaskDefinitionDTO> updateTaskDefinition(@PathVariable Long studyId,
                                                                       @PathVariable Long taskId,
                                                                       @RequestBody UpdateStudyTaskDefinitionRequest request,
                                                                       Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure tasks."
        );
        StudyTaskDefinitionEntity definition = studyTaskDefinitionRepository.findByIdAndStudy(taskId, study)
                .orElseThrow(() -> new RuntimeException("Task definition not found."));
        if (request.instructions() != null) {
            definition.setInstructions(request.instructions());
        }
        if (request.artifactIds() != null) {
            List<ArtifactEntity> artifacts = resolveArtifactsForTask(study, request.artifactIds());
            applyArtifacts(definition, artifacts);
        }
        if (request.ratingCriterionIds() != null) {
            List<StudyRatingCriterionEntity> criteria = resolveRatingCriteriaForTask(study, request.ratingCriterionIds());
            applyRatingCriteria(definition, criteria);
        }
        StudyTaskDefinitionEntity saved = studyTaskDefinitionRepository.save(definition);
        markUnpublishedChanges(study);
        studyAuditService.record(study, actor, StudyAuditAction.STUDY_UPDATED, Map.of(
                "taskDefinitionId", saved.getId(),
                "change", "TASK_UPDATED"
        ));
        Map<Long, String> aliasMap = loadArtifactAliasMap(study);
        return ResponseEntity.ok(convertTaskDefinition(saved, aliasMap));
    }

    @DeleteMapping("/{studyId}/task-definitions/{taskId}")
    public ResponseEntity<?> deleteTaskDefinition(@PathVariable Long studyId,
                                                  @PathVariable Long taskId,
                                                  Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure tasks."
        );
        StudyTaskDefinitionEntity definition = studyTaskDefinitionRepository.findByIdAndStudy(taskId, study)
                .orElseThrow(() -> new RuntimeException("Task definition not found."));
        studyTaskDefinitionRepository.delete(definition);
        renumberTaskDefinitions(study);
        markUnpublishedChanges(study);
        studyAuditService.record(study, actor, StudyAuditAction.STUDY_UPDATED, Map.of(
                "taskDefinitionId", taskId,
                "change", "TASK_REMOVED"
        ));
        return ResponseEntity.ok(Map.of("message", "Task definition removed."));
    }

    @PostMapping("/{studyId}/task-definitions/reorder")
    public ResponseEntity<?> reorderTaskDefinitions(@PathVariable Long studyId,
                                                    @RequestBody ReorderStudyTasksRequest request,
                                                    Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.EDIT_DRAFT,
                "You do not have permission to configure tasks."
        );
        if (request.orderedTaskIds() == null || request.orderedTaskIds().isEmpty()) {
            throw new IllegalArgumentException("orderedTaskIds is required.");
        }
        List<StudyTaskDefinitionEntity> definitions = studyTaskDefinitionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study);
        if (definitions.size() != request.orderedTaskIds().size()) {
            throw new IllegalArgumentException("Reorder list must include all task IDs.");
        }
        Map<Long, StudyTaskDefinitionEntity> byId = definitions.stream()
                .collect(Collectors.toMap(StudyTaskDefinitionEntity::getId, def -> def));
        int sortOrder = 0;
        for (Long id : request.orderedTaskIds()) {
            StudyTaskDefinitionEntity def = byId.get(id);
            if (def == null) {
                throw new IllegalArgumentException("Task ID " + id + " does not exist for this study.");
            }
            def.setSortOrder(sortOrder++);
        }
        studyTaskDefinitionRepository.saveAll(definitions);
        markUnpublishedChanges(study);
        studyAuditService.record(study, actor, StudyAuditAction.STUDY_UPDATED, Map.of(
                "change", "TASK_REORDERED"
        ));
        return ResponseEntity.ok(Map.of("message", "Task order updated."));
    }

    @GetMapping("/{studyId}/audit-log")
    public ResponseEntity<List<StudyAuditLogDTO>> getAuditLog(@PathVariable Long studyId,
                                                              @RequestParam(required = false) StudyAuditAction action,
                                                              @RequestParam(required = false) Long actorId,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                              @RequestParam(defaultValue = "100") int limit,
                                                              Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view audit logs."
        );
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        List<StudyAuditLogEntity> logs = studyAuditLogRepository.searchLogs(
                study,
                action,
                actorId,
                from,
                to,
                PageRequest.of(0, boundedLimit)
        );
        return ResponseEntity.ok(convertAuditLogs(logs));
    }

    @GetMapping("/{studyId}/audit-log/export")
    public ResponseEntity<byte[]> exportAuditLog(@PathVariable Long studyId,
                                                 @RequestParam(required = false) StudyAuditAction action,
                                                 @RequestParam(required = false) Long actorId,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                 Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to export audit logs."
        );
        List<StudyAuditLogEntity> logs = studyAuditLogRepository.searchLogs(
                study,
                action,
                actorId,
                from,
                to,
                PageRequest.of(0, 1000)
        );
        byte[] csvBytes = buildAuditCsv(convertAuditLogs(logs)).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"study-" + studyId + "-audit-log.csv\"");
        studyAuditService.record(study, actor, StudyAuditAction.AUDIT_LOG_EXPORTED, Map.of(
                "rowCount", logs.size()
        ));
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @PostMapping("/{studyId}/archive")
    public ResponseEntity<byte[]> archiveStudy(@PathVariable Long studyId, Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.ARCHIVE,
                "You do not have permission to archive this study."
        );
        byte[] archive = studyArchiveService.buildArchive(study);
        studyAuditService.record(study, actor, StudyAuditAction.STUDY_ARCHIVED, Map.of(
                "bytes", archive.length
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"study-" + studyId + "-archive.zip\"");
        return ResponseEntity.ok()
                .headers(headers)
                .body(archive);
    }

    @PostMapping("/{studyId}/clone")
    public ResponseEntity<?> cloneStudy(@PathVariable Long studyId,
                                        @RequestBody CloneStudyRequest request,
                                        Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view this study."
        );
        String title = StringUtils.hasText(request.title())
                ? request.title().trim()
                : study.getTitle() + " Copy";
        StudyEntity cloned = studyCloneService.cloneStudy(study, actor, title, request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Study cloned successfully.",
                "studyId", cloned.getId()
        ));
    }

    @GetMapping("/{studyId}/versions")
    public ResponseEntity<List<StudyVersionSummaryDTO>> getStudyVersions(@PathVariable Long studyId,
                                                                         Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view versions."
        );
        List<StudyVersionSummaryDTO> versions = studyVersionRepository.findAllByStudyOrderByVersionNumberDesc(study)
                .stream()
                .map(version -> new StudyVersionSummaryDTO(
                        version.getVersionNumber(),
                        version.getCreatedAt(),
                        version.getPublishedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{studyId}/versions/{versionNumber}")
    public ResponseEntity<StudyVersionDetailDTO> getStudyVersionDetail(@PathVariable Long studyId,
                                                                       @PathVariable int versionNumber,
                                                                       Principal principal) {
        UserEntity actor = getCurrentUser(principal);
        StudyEntity study = getStudyOrThrow(studyId);
        studyPermissionService.requirePermission(
                study,
                actor,
                StudyPermissionAction.VIEW,
                "You do not have permission to view versions."
        );
        StudyVersionEntity version = studyVersionRepository.findByStudyAndVersionNumber(study, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found."));
        JsonNode config;
        try {
            config = versionObjectMapper.readTree(version.getConfigJson());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse version snapshot", e);
        }
        return ResponseEntity.ok(new StudyVersionDetailDTO(
                version.getVersionNumber(),
                version.getCreatedAt(),
                version.getPublishedAt(),
                config
        ));
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
                study.getAccessWindowEnd(),
                study.getProvenanceNote()
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

    private List<StudyAuditLogDTO> convertAuditLogs(List<StudyAuditLogEntity> logs) {
        return logs.stream()
                .map(entry -> new StudyAuditLogDTO(
                        entry.getId(),
                        entry.getAction(),
                        entry.getActor() != null ? entry.getActor().getId() : null,
                        entry.getActor() != null ? entry.getActor().getName() : "System",
                        parseJson(entry.getDetailsJson()),
                        entry.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private String buildAuditCsv(List<StudyAuditLogDTO> logs) {
        StringBuilder builder = new StringBuilder();
        builder.append("id,action,actorId,actorName,createdAt,details").append('\n');
        for (StudyAuditLogDTO log : logs) {
            builder.append(escapeCsv(String.valueOf(log.id()))).append(',')
                    .append(escapeCsv(log.action().name())).append(',')
                    .append(escapeCsv(log.actorId() != null ? log.actorId().toString() : "")).append(',')
                    .append(escapeCsv(log.actorName())).append(',')
                    .append(escapeCsv(log.createdAt() != null ? log.createdAt().toString() : "")).append(',')
                    .append(escapeCsv(log.details() != null ? log.details().toString() : ""))
                    .append('\n');
        }
        return builder.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace("\"", "\"\"");
        if (sanitized.contains(",") || sanitized.contains("\n")) {
            return "\"" + sanitized + "\"";
        }
        return sanitized;
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return versionObjectMapper.nullNode();
        }
        try {
            return versionObjectMapper.readTree(json);
        } catch (Exception e) {
            return versionObjectMapper.createObjectNode().put("unparseable", true);
        }
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

    private StudyArtifactDTO convertArtifactSelection(StudyArtifactSelectionEntity selection) {
        ArtifactEntity artifact = selection.getArtifact();
        String ownerName = userRepository.findById(artifact.getOwnerId())
                .map(UserEntity::getName)
                .orElse("Unknown uploader");
        return new StudyArtifactDTO(
                selection.getId(),
                artifact.getId(),
                artifact.getFileName(),
                artifact.getMimeType(),
                selection.getAlias(),
                ownerName,
                artifact.getCreatedAt(),
                selection.getCreatedAt()
        );
    }

    private StudyRatingCriterionDTO convertCriterion(StudyRatingCriterionEntity entity) {
        return new StudyRatingCriterionDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getWeight(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private void ensureNotAlreadyParticipant(StudyEntity study, String normalizedEmail) {
        if (normalizedEmail == null) {
            return;
        }
        boolean enrolled = study.getParticipants().stream()
                .anyMatch(participant -> participant.getEmail() != null
                        && participant.getEmail().equalsIgnoreCase(normalizedEmail));
        if (enrolled) {
            throw new IllegalStateException("This email already belongs to an enrolled participant.");
        }
    }

    private Map<String, Object> enrichWithId(Map<String, Object> payload, Long criterionId) {
        Map<String, Object> copy = new HashMap<>(payload);
        copy.putIfAbsent("criterionId", criterionId);
        return copy;
    }

    private StudyTaskDefinitionDTO convertTaskDefinition(StudyTaskDefinitionEntity entity,
                                                         Map<Long, String> aliasMap) {
        List<StudyTaskArtifactDTO> artifactDTOs = entity.getArtifacts().stream()
                .map(artifact -> new StudyTaskArtifactDTO(
                        artifact.getArtifact().getId(),
                        artifact.getArtifact().getFileName(),
                        artifact.getArtifact().getMimeType(),
                        aliasMap.getOrDefault(artifact.getArtifact().getId(), artifact.getArtifact().getFileName()),
                        artifact.getPosition()
                ))
                .collect(Collectors.toList());
        List<StudyRatingCriterionDTO> criteria = entity.getRatingCriteria().stream()
                .map(mapping -> convertCriterion(mapping.getRatingCriterion()))
                .collect(Collectors.toList());
        return new StudyTaskDefinitionDTO(
                entity.getId(),
                entity.getInstructions(),
                entity.getSortOrder(),
                artifactDTOs,
                criteria,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void applyArtifactsAndCriteria(StudyTaskDefinitionEntity definition,
                                           List<ArtifactEntity> artifacts,
                                           List<StudyRatingCriterionEntity> criteria) {
        applyArtifacts(definition, artifacts);
        applyRatingCriteria(definition, criteria);
    }

    private void applyArtifacts(StudyTaskDefinitionEntity definition, List<ArtifactEntity> artifacts) {
        definition.getArtifacts().clear();
        int position = 0;
        for (ArtifactEntity artifact : artifacts) {
            definition.getArtifacts().add(new StudyTaskArtifactEntity(definition, artifact, position++));
        }
    }

    private void applyRatingCriteria(StudyTaskDefinitionEntity definition,
                                     List<StudyRatingCriterionEntity> criteria) {
        definition.getRatingCriteria().clear();
        for (StudyRatingCriterionEntity criterion : criteria) {
            definition.getRatingCriteria().add(new StudyTaskRatingCriterionEntity(definition, criterion));
        }
    }

    private Map<Long, String> loadArtifactAliasMap(StudyEntity study) {
        return studyArtifactSelectionRepository.findByStudyOrderByCreatedAtAsc(study)
                .stream()
                .collect(Collectors.toMap(
                        selection -> selection.getArtifact().getId(),
                        selection -> selection.getAlias() != null ? selection.getAlias() : selection.getArtifact().getFileName(),
                        (left, right) -> left
                ));
    }

    private List<ArtifactEntity> resolveArtifactsForTask(StudyEntity study, List<Long> artifactIds) {
        if (artifactIds == null || artifactIds.size() < 2 || artifactIds.size() > 3) {
            throw new IllegalArgumentException("Each task must include 2 or 3 artifacts.");
        }
        Set<Long> selectedArtifactIds = studyArtifactSelectionRepository.findByStudyOrderByCreatedAtAsc(study)
                .stream()
                .map(selection -> selection.getArtifact().getId())
                .collect(Collectors.toSet());
        List<ArtifactEntity> artifacts = artifactRepository.findAllById(artifactIds);
        if (artifacts.size() != artifactIds.size()) {
            throw new IllegalArgumentException("One or more artifacts could not be found.");
        }
        for (Long artifactId : artifactIds) {
            if (!selectedArtifactIds.contains(artifactId)) {
                throw new IllegalArgumentException("Artifact " + artifactId + " is not linked to this study.");
            }
        }
        Map<Long, ArtifactEntity> byId = artifacts.stream()
                .collect(Collectors.toMap(ArtifactEntity::getId, artifact -> artifact));
        return artifactIds.stream()
                .map(byId::get)
                .collect(Collectors.toList());
    }

    private List<StudyRatingCriterionEntity> resolveRatingCriteriaForTask(StudyEntity study,
                                                                          List<Long> criterionIds) {
        if (criterionIds == null || criterionIds.isEmpty()) {
            return List.of();
        }
        List<StudyRatingCriterionEntity> all = studyRatingCriterionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study);
        Map<Long, StudyRatingCriterionEntity> byId = all.stream()
                .collect(Collectors.toMap(StudyRatingCriterionEntity::getId, criterion -> criterion));
        List<StudyRatingCriterionEntity> resolved = new ArrayList<>();
        for (Long id : criterionIds) {
            StudyRatingCriterionEntity entity = byId.get(id);
            if (entity == null) {
                throw new IllegalArgumentException("Rating criterion " + id + " does not belong to this study.");
            }
            resolved.add(entity);
        }
        return resolved;
    }

    private void renumberTaskDefinitions(StudyEntity study) {
        List<StudyTaskDefinitionEntity> definitions = studyTaskDefinitionRepository.findByStudyOrderBySortOrderAscCreatedAtAsc(study);
        int order = 0;
        for (StudyTaskDefinitionEntity definition : definitions) {
            definition.setSortOrder(order++);
        }
        studyTaskDefinitionRepository.saveAll(definitions);
    }

    private void markUnpublishedChanges(StudyEntity study) {
        if (!study.isHasUnpublishedChanges()) {
            study.setHasUnpublishedChanges(true);
            studyRepository.save(study);
        }
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
