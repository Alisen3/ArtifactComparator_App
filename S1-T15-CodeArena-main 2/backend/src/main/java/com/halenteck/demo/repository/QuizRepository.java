// Dosya Yolu: demo/QuizRepository.java
package com.halenteck.demo.repository;

import com.halenteck.demo.entity.QuizEntity;
import com.halenteck.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<QuizEntity, Long> {

    // Araştırmacının "Benim Kuislerim" sayfasını beslemek için gerekli
    List<QuizEntity> findByCreator(UserEntity creator);
}