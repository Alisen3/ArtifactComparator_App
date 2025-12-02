package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyInviteEntity;
import com.halenteck.demo.entity.StudyInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudyInviteRepository extends JpaRepository<StudyInviteEntity, Long> {

    List<StudyInviteEntity> findByStudyOrderByCreatedAtDesc(StudyEntity study);

    Optional<StudyInviteEntity> findByToken(String token);

    Optional<StudyInviteEntity> findFirstByStudyAndEmailAndStatusIn(StudyEntity study,
                                                                    String email,
                                                                    List<StudyInviteStatus> statuses);

    List<StudyInviteEntity> findByStudyAndStatusAndExpiresAtBefore(StudyEntity study,
                                                                   StudyInviteStatus status,
                                                                   LocalDateTime cutoff);
}

