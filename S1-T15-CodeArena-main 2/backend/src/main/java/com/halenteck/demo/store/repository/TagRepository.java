package com.halenteck.demo.store.repository;

import com.halenteck.demo.store.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, Long> {

    // Etiket ismine göre arama (Controller'da kullanılıyor)
    Optional<TagEntity> findByName(String name);
}