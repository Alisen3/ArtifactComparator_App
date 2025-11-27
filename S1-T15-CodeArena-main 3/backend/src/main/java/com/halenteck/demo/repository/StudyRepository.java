package com.halenteck.demo.repository;

import com.halenteck.demo.entity.StudyEntity;
import com.halenteck.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyRepository extends JpaRepository<StudyEntity, Long> {

    List<StudyEntity> findByCreator(UserEntity creator);
}