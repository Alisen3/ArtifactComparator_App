// Dosya Yolu: demo/QuestionRepository.java
package com.halenteck.demo.repository;

import com.halenteck.demo.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    // İleride ihtiyaç olursa buraya özel sorgular eklenebilir.
}