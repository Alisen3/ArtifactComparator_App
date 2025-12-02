package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyCollaboratorEntity;
import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.permission.StudyCollaboratorRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyCollaboratorRepository extends JpaRepository<StudyCollaboratorEntity, Long> {

    Optional<StudyCollaboratorEntity> findByStudyAndCollaborator(StudyEntity study, UserEntity collaborator);

    List<StudyCollaboratorEntity> findAllByStudy(StudyEntity study);

    List<StudyCollaboratorEntity> findAllByCollaborator(UserEntity collaborator);

    long countByStudyAndRole(StudyEntity study, StudyCollaboratorRole role);

    @Query("select c from StudyCollaboratorEntity c where c.study.id = :studyId and c.collaborator.id = :userId")
    Optional<StudyCollaboratorEntity> findByStudyIdAndCollaboratorId(@Param("studyId") Long studyId,
                                                                     @Param("userId") Long userId);
}


