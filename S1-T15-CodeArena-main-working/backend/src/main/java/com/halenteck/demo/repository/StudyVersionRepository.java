package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.StudyVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyVersionRepository extends JpaRepository<StudyVersionEntity, Long> {

    Optional<StudyVersionEntity> findTopByStudyOrderByVersionNumberDesc(StudyEntity study);

    Optional<StudyVersionEntity> findByStudyAndVersionNumber(StudyEntity study, int versionNumber);

    List<StudyVersionEntity> findAllByStudyOrderByVersionNumberDesc(StudyEntity study);
}


