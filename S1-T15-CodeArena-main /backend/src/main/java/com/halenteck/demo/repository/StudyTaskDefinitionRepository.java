package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyTaskDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyTaskDefinitionRepository extends JpaRepository<StudyTaskDefinitionEntity, Long> {

    List<StudyTaskDefinitionEntity> findByStudyOrderBySortOrderAscCreatedAtAsc(StudyEntity study);

    Optional<StudyTaskDefinitionEntity> findByIdAndStudy(Long id, StudyEntity study);

    long countByStudy(StudyEntity study);
}

