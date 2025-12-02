package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyTemplateEntity;
import com.halenteck.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyTemplateRepository extends JpaRepository<StudyTemplateEntity, Long> {

    List<StudyTemplateEntity> findByOwnerOrderByUpdatedAtDesc(UserEntity owner);

    Optional<StudyTemplateEntity> findByIdAndOwner(Long id, UserEntity owner);
}

