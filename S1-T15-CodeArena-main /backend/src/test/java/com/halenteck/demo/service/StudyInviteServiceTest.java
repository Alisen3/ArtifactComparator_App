package com.halenteck.demo.service;

import com.halenteck.demo.UserRole;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyInviteDeliveryMethod;
import com.halenteck.demo.entity.StudyInviteEntity;
import com.halenteck.demo.entity.StudyInviteStatus;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.StudyInviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyInviteServiceTest {

    @Mock
    private StudyInviteRepository studyInviteRepository;

    @InjectMocks
    private StudyInviteService studyInviteService;

    private StudyEntity study;
    private UserEntity owner;

    @BeforeEach
    void setUp() throws Exception {
        owner = new UserEntity("Owner", "owner@example.com", "pw", UserRole.RESEARCHER);
        study = new StudyEntity("Title", "Desc", false, owner);
        setId(study, 1L);
    }

    @Test
    void ensureNoDuplicateInviteThrowsWhenPendingExists() {
        StudyInviteEntity existing = new StudyInviteEntity(study, owner, "user@example.com", StudyInviteDeliveryMethod.EMAIL, LocalDateTime.now().plusDays(1));
        when(studyInviteRepository.findFirstByStudyAndEmailAndStatusIn(eq(study), eq("user@example.com"), any()))
                .thenReturn(Optional.of(existing));
        assertThrows(IllegalStateException.class, () -> studyInviteService.ensureNoDuplicateInvite(study, "user@example.com"));
    }

    @Test
    void finalizeAcceptanceMarksAccepted() throws Exception {
        UserEntity participant = new UserEntity("Participant", "user@example.com", "pw", UserRole.PARTICIPANT);
        setUserId(participant, 5L);
        StudyInviteEntity invite = new StudyInviteEntity(study, owner, "user@example.com", StudyInviteDeliveryMethod.EMAIL, LocalDateTime.now().plusDays(1));
        setInviteToken(invite, "token123");
        when(studyInviteRepository.findByToken("token123")).thenReturn(Optional.of(invite));
        when(studyInviteRepository.save(invite)).thenReturn(invite);

        StudyInviteEntity result = studyInviteService.getValidInvite("token123");
        studyInviteService.finalizeAcceptance(result, participant);

        assertEquals(StudyInviteStatus.ACCEPTED, invite.getStatus());
        assertTrue(study.getParticipants().contains(participant));
        assertEquals(participant, invite.getInvitedUser());
        verify(studyInviteRepository).findByToken("token123");
        verify(studyInviteRepository).save(invite);
    }

    private void setInviteToken(StudyInviteEntity invite, String token) throws Exception {
        Field field = StudyInviteEntity.class.getDeclaredField("token");
        field.setAccessible(true);
        field.set(invite, token);
    }

    private void setId(StudyEntity entity, Long id) throws Exception {
        Field field = StudyEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    private void setUserId(UserEntity entity, Long id) throws Exception {
        Field field = UserEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}

