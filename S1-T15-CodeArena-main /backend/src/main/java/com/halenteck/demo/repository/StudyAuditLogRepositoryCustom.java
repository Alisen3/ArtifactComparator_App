package com.halenteck.demo.repository;

import com.halenteck.demo.audit.StudyAuditAction;
import com.halenteck.demo.entity.StudyAuditLogEntity;
import com.halenteck.demo.entity.StudyEntity;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyAuditLogRepositoryCustom {

    List<StudyAuditLogEntity> searchLogs(StudyEntity study,
                                         StudyAuditAction action,
                                         Long actorId,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         Pageable pageable);
}

