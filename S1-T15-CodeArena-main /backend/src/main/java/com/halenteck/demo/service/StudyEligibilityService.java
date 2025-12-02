package com.halenteck.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halenteck.demo.dto.*;
import com.halenteck.demo.entity.*;
import com.halenteck.demo.repository.StudyEnrollmentRequestRepository;
import com.halenteck.demo.repository.StudyInviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudyEligibilityService {

    private final EligibilityRuleEngine ruleEngine;
    private final StudyEnrollmentRequestRepository enrollmentRequestRepository;
    private final StudyInviteService studyInviteService;
    private final StudyInviteRepository studyInviteRepository;
    private final ObjectMapper objectMapper;

    public StudyEligibilityService(EligibilityRuleEngine ruleEngine,
                                   StudyEnrollmentRequestRepository enrollmentRequestRepository,
                                   StudyInviteService studyInviteService,
                                   StudyInviteRepository studyInviteRepository) {
        this.ruleEngine = ruleEngine;
        this.enrollmentRequestRepository = enrollmentRequestRepository;
        this.studyInviteService = studyInviteService;
        this.studyInviteRepository = studyInviteRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public EligibilityOverviewDTO describeEligibility(StudyEntity study) {
        EligibilityConfigDTO config = getConfig(study);
        Map<String, Integer> stats = computeStats(study, config);
        List<StudyEnrollmentRequestDTO> pending = enrollmentRequestRepository.findByStudyAndStatusOrderByCreatedAtAsc(
                        study, StudyEnrollmentStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new EligibilityOverviewDTO(config, stats, pending);
    }

    public EligibilityConfigDTO getConfig(StudyEntity study) {
        return ruleEngine.parseConfig(study.getEligibilityRulesJson());
    }

    public void updateConfig(StudyEntity study, EligibilityConfigDTO config) {
        try {
            study.setEligibilityRulesJson(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid eligibility configuration", e);
        }
        if (config.approvalMode() != null) {
            study.setEligibilityApprovalMode(config.approvalMode());
        }
    }

    public EligibilityEvaluationResult evaluateCandidate(StudyEntity study, UserEntity candidate) {
        EligibilityConfigDTO config = getConfig(study);
        Map<String, Object> result = ruleEngine.evaluate(candidate, config);
        boolean eligible = (boolean) result.getOrDefault("eligible", true);
        return new EligibilityEvaluationResult(eligible, eligible ? "Eligible" : "Candidate does not meet eligibility requirements.");
    }

    public StudyEnrollmentRequestEntity createPendingRequest(StudyEntity study,
                                                             UserEntity participant,
                                                             StudyInviteEntity invite,
                                                             EligibilityEvaluationResult evaluation) {
        try {
            String snapshot = objectMapper.writeValueAsString(Map.of(
                    "eligible", evaluation.eligible(),
                    "reason", evaluation.reason()
            ));
            StudyEnrollmentRequestEntity requestEntity = new StudyEnrollmentRequestEntity(study, participant, invite, snapshot);
            return enrollmentRequestRepository.save(requestEntity);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to snapshot eligibility result", e);
        }
    }

    @Transactional
    public void approveRequest(StudyEnrollmentRequestEntity request, UserEntity reviewer) {
        if (request.getStatus() != StudyEnrollmentStatus.PENDING) {
            throw new IllegalStateException("Request already resolved.");
        }
        if (request.getInvite() != null) {
            studyInviteService.finalizeAcceptance(request.getInvite(), request.getParticipant());
        } else {
            request.getStudy().addParticipant(request.getParticipant());
        }
        request.setStatus(StudyEnrollmentStatus.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setReviewedBy(reviewer);
        enrollmentRequestRepository.save(request);
    }

    @Transactional
    public void rejectRequest(StudyEnrollmentRequestEntity request, UserEntity reviewer, String note) {
        if (request.getStatus() != StudyEnrollmentStatus.PENDING) {
            throw new IllegalStateException("Request already resolved.");
        }
        request.setStatus(StudyEnrollmentStatus.REJECTED);
        request.setResolvedAt(LocalDateTime.now());
        request.setReviewedBy(reviewer);
        request.setReviewerNote(note);
        enrollmentRequestRepository.save(request);
        if (request.getInvite() != null) {
            request.getInvite().setStatus(StudyInviteStatus.REVOKED);
            studyInviteRepository.save(request.getInvite());
        }
    }

    public StudyEnrollmentRequestEntity getRequestOrThrow(Long requestId) {
        return enrollmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment request not found."));
    }

    public List<StudyEnrollmentRequestDTO> listRequests(StudyEntity study) {
        return enrollmentRequestRepository.findByStudyOrderByCreatedAtDesc(study)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private StudyEnrollmentRequestDTO toDto(StudyEnrollmentRequestEntity entity) {
        return new StudyEnrollmentRequestDTO(
                entity.getId(),
                entity.getParticipant().getId(),
                entity.getParticipant().getName(),
                entity.getParticipant().getEmail(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getResolvedAt(),
                entity.getReviewedBy() != null ? entity.getReviewedBy().getName() : null,
                entity.getReviewerNote()
        );
    }

    private Map<String, Integer> computeStats(StudyEntity study, EligibilityConfigDTO config) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("eligible", 0);
        stats.put("ineligible", 0);
        stats.put("pending", (int) enrollmentRequestRepository.findByStudyAndStatusOrderByCreatedAtAsc(
                        study, StudyEnrollmentStatus.PENDING).size());
        study.getParticipants().forEach(participant -> {
            EligibilityEvaluationResult eval = evaluateCandidate(study, participant);
            if (eval.eligible()) {
                stats.computeIfPresent("eligible", (k, v) -> v + 1);
            } else {
                stats.computeIfPresent("ineligible", (k, v) -> v + 1);
            }
        });
        return stats;
    }
}

