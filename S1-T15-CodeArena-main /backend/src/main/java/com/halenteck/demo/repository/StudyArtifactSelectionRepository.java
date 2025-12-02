package com.halenteck.demo.repository;

import com.halenteck.demo.entity.ArtifactEntity;
import com.halenteck.demo.entity.StudyArtifactSelectionEntity;
import com.halenteck.demo.entity.StudyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyArtifactSelectionRepository extends JpaRepository<StudyArtifactSelectionEntity, Long> {

    List<StudyArtifactSelectionEntity> findByStudyOrderByCreatedAtAsc(StudyEntity study);

    long countByStudy(StudyEntity study);

    Optional<StudyArtifactSelectionEntity> findByIdAndStudy(Long id, StudyEntity study);

    Optional<StudyArtifactSelectionEntity> findByStudyAndArtifact(StudyEntity study, ArtifactEntity artifact);
}

