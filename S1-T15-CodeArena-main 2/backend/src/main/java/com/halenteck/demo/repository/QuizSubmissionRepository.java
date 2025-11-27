// Dosya Yolu: demo/QuizSubmissionRepository.java
package com.halenteck.demo.repository;

import com.halenteck.demo.entity.QuizEntity;
import com.halenteck.demo.entity.QuizSubmissionEntity;
import com.halenteck.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmissionEntity, Long> {

    // Bir çalışmadaki tüm katılımcıların notlarını görmek için (Issue #9, #10)
    List<QuizSubmissionEntity> findByQuiz(QuizEntity quiz);

    // Bir katılımcının tüm kuis 'submission'larını görmek için
    List<QuizSubmissionEntity> findByParticipant(UserEntity participant);

    // Bir katılımcının belirli bir kuis'i daha önce alıp almadığını
    // veya kuis'i başlatıp başlatmadığını (Issue #12) kontrol etmek için
    Optional<QuizSubmissionEntity> findByQuizAndParticipant(QuizEntity quiz, UserEntity participant);
}