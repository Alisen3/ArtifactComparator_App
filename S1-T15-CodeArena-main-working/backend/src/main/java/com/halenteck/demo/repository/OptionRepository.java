// Dosya Yolu: demo/OptionRepository.java
package com.halenteck.demo.repository;

import com.halenteck.demo.entity.OptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionRepository extends JpaRepository<OptionEntity, Long> {
    // İleride ihtiyaç olursa buraya özel sorgular eklenebilir.
}