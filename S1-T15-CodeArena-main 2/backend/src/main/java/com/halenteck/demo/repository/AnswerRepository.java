// Dosya Yolu: demo/AnswerRepository.java
package com.halenteck.demo.repository;

import com.halenteck.demo.entity.AnswerEntity;
import com.halenteck.demo.entity.QuizSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {

    // Bir 'submission'a ait tüm cevapları getirmek için
    List<AnswerEntity> findBySubmission(QuizSubmissionEntity submission);
}