package com.halenteck.demo.store.repository;

import com.halenteck.demo.store.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long> {
    
    // Belirli bir kullanıcının tüm klasörlerini listelemek için
    List<FolderEntity> findByOwnerId(Long ownerId);
    
    // Kullanıcının belirli bir isimdeki klasörünü bulmak için
    boolean existsByOwnerIdAndName(Long ownerId, String name);
}