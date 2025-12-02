package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyAuditLogEntity;
import com.halenteck.demo.entity.StudyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyAuditLogRepository extends JpaRepository<StudyAuditLogEntity, Long> {

    List<StudyAuditLogEntity> findAllByStudyOrderByCreatedAtDesc(StudyEntity study);
}


