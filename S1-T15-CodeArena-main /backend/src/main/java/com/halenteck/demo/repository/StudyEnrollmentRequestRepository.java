package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyEnrollmentRequestEntity;
import com.halenteck.demo.entity.StudyEnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyEnrollmentRequestRepository extends JpaRepository<StudyEnrollmentRequestEntity, Long> {

    List<StudyEnrollmentRequestEntity> findByStudyAndStatusOrderByCreatedAtAsc(StudyEntity study,
                                                                              StudyEnrollmentStatus status);

    List<StudyEnrollmentRequestEntity> findByStudyOrderByCreatedAtDesc(StudyEntity study);
}

