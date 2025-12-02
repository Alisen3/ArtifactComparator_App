package com.halenteck.demo.repository;

import com.halenteck.demo.entity.ComparisonTaskEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComparisonTaskRepository extends JpaRepository<ComparisonTaskEntity, Long> {

    List<ComparisonTaskEntity> findByParticipant(UserEntity participant);

    List<ComparisonTaskEntity> findByStudy(StudyEntity study);

    long countByStudy(StudyEntity study);

    List<ComparisonTaskEntity> findByStudyAndStudyVersionIsNull(StudyEntity study);
}