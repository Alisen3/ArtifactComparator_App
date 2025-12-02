package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyRatingCriterionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyRatingCriterionRepository extends JpaRepository<StudyRatingCriterionEntity, Long> {

    List<StudyRatingCriterionEntity> findByStudyOrderBySortOrderAscCreatedAtAsc(StudyEntity study);

    long countByStudy(StudyEntity study);

    Optional<StudyRatingCriterionEntity> findByIdAndStudy(Long id, StudyEntity study);
}

