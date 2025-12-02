package com.halenteck.demo.service;

import com.halenteck.demo.UserRole;
import com.halenteck.demo.dto.EligibilityConfigDTO;
import com.halenteck.demo.dto.EligibilityRuleDTO;
import com.halenteck.demo.dto.EligibilityRulesetDTO;
import com.halenteck.demo.entity.*;
import com.halenteck.demo.repository.StudyEnrollmentRequestRepository;
import com.halenteck.demo.repository.StudyInviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyEligibilityServiceTest {

    @Mock
    private StudyEnrollmentRequestRepository enrollmentRequestRepository;
    @Mock
    private StudyInviteService studyInviteService;
    @Mock
    private StudyInviteRepository studyInviteRepository;

    private StudyEligibilityService studyEligibilityService;
    private StudyEntity study;

    @BeforeEach
    void setUp() {
        studyEligibilityService = new StudyEligibilityService(new EligibilityRuleEngine(), enrollmentRequestRepository, studyInviteService, studyInviteRepository);
        UserEntity owner = new UserEntity("Owner", "owner@example.com", "pw", UserRole.RESEARCHER);
        study = new StudyEntity("Title", "Desc", false, owner);
    }

    @Test
    void evaluateCandidateHonorsSkillsRule() {
        EligibilityConfigDTO config = new EligibilityConfigDTO(
                EligibilityApprovalMode.AUTO,
                List.of(new EligibilityRulesetDTO("AND", List.of(new EligibilityRuleDTO(
                        "skills",
                        "contains",
                        "React"
                ))))
        );
        studyEligibilityService.updateConfig(study, config);
        UserEntity participant = new UserEntity("Dev", "dev@example.com", "pw", UserRole.PARTICIPANT);
        participant.setSkills("React,Node");

        var result = studyEligibilityService.evaluateCandidate(study, participant);

        assertTrue(result.eligible());
    }

    @Test
    void approveRequestFinalizesInvite() {
        UserEntity reviewer = new UserEntity("Reviewer", "rev@example.com", "pw", UserRole.RESEARCHER);
        UserEntity participant = new UserEntity("Candidate", "cand@example.com", "pw", UserRole.PARTICIPANT);
        StudyInviteEntity invite = mock(StudyInviteEntity.class);
        StudyEnrollmentRequestEntity request = new StudyEnrollmentRequestEntity(study, participant, invite, "{}");

        studyEligibilityService.approveRequest(request, reviewer);

        verify(studyInviteService).finalizeAcceptance(invite, participant);
        assertEquals(StudyEnrollmentStatus.APPROVED, request.getStatus());
        assertNotNull(request.getResolvedAt());
    }
}

