package com.halenteck.demo.service;

import com.halenteck.demo.dto.StudyInviteDTO;
import com.halenteck.demo.entity.*;
import com.halenteck.demo.repository.StudyInviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudyInviteService {

    private static final int DEFAULT_EXPIRY_HOURS = 72;

    private final StudyInviteRepository studyInviteRepository;

    public StudyInviteService(StudyInviteRepository studyInviteRepository) {
        this.studyInviteRepository = studyInviteRepository;
    }

    public List<StudyInviteDTO> listInvites(StudyEntity study) {
        expireOutdatedInvites(study);
        return studyInviteRepository.findByStudyOrderByCreatedAtDesc(study)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public StudyInviteEntity createInvite(StudyEntity study,
                                          UserEntity actor,
                                          String email,
                                          StudyInviteDeliveryMethod method,
                                          Integer expiresInHours,
                                          UserEntity invitedUser) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiresInHours != null ? expiresInHours : DEFAULT_EXPIRY_HOURS);
        StudyInviteEntity invite = new StudyInviteEntity(
                study,
                actor,
                email,
                method,
                expiresAt
        );
        invite.setInvitedUser(invitedUser);
        return studyInviteRepository.save(invite);
    }

    public StudyInviteEntity getValidInvite(String token) {
        StudyInviteEntity invite = studyInviteRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invite token not found."));
        validatePendingInvite(invite);
        return invite;
    }

    @Transactional
    public void markUnderReview(StudyInviteEntity invite, UserEntity participant) {
        invite.setStatus(StudyInviteStatus.UNDER_REVIEW);
        invite.setInvitedUser(participant);
        invite.setAcceptedBy(participant);
        studyInviteRepository.save(invite);
    }

    @Transactional
    public void finalizeAcceptance(StudyInviteEntity invite, UserEntity participant) {
        invite.setStatus(StudyInviteStatus.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(participant);
        invite.setInvitedUser(participant);
        invite.getStudy().addParticipant(participant);
        studyInviteRepository.save(invite);
    }

    public void expireOutdatedInvites(StudyEntity study) {
        List<StudyInviteEntity> expired = studyInviteRepository.findByStudyAndStatusAndExpiresAtBefore(
                study,
                StudyInviteStatus.PENDING,
                LocalDateTime.now()
        );
        if (!expired.isEmpty()) {
            expired.forEach(invite -> invite.setStatus(StudyInviteStatus.EXPIRED));
            studyInviteRepository.saveAll(expired);
        }
    }

    public StudyInviteDTO toDto(StudyInviteEntity invite) {
        return new StudyInviteDTO(
                invite.getId(),
                invite.getEmail(),
                invite.getInvitedUser() != null ? invite.getInvitedUser().getId() : null,
                invite.getInvitedUser() != null ? invite.getInvitedUser().getName() : null,
                invite.getDeliveryMethod(),
                invite.getStatus(),
                invite.getToken(),
                invite.getExpiresAt(),
                invite.getCreatedAt(),
                invite.getAcceptedAt(),
                invite.getAcceptedBy() != null ? invite.getAcceptedBy().getName() : null
        );
    }

    public void ensureNoDuplicateInvite(StudyEntity study, String email) {
        if (email == null) {
            return;
        }
        studyInviteRepository.findFirstByStudyAndEmailAndStatusIn(
                        study,
                        email,
                        List.of(StudyInviteStatus.PENDING, StudyInviteStatus.UNDER_REVIEW))
                .ifPresent(invite -> {
                    throw new IllegalStateException("An invite is already pending for this email.");
                });
    }

    private void validatePendingInvite(StudyInviteEntity invite) {
        if (invite.getStatus() != StudyInviteStatus.PENDING) {
            throw new IllegalStateException("Invite is no longer available.");
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus(StudyInviteStatus.EXPIRED);
            studyInviteRepository.save(invite);
            throw new IllegalStateException("Invite token has expired.");
        }
    }
}

