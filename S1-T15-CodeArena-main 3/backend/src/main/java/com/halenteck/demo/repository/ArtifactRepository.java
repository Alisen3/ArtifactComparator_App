package com.halenteck.demo.repository;

import com.halenteck.demo.entity.ArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, Long> { 
    
    // Bu metot 'upload' için gerekli
    boolean existsByOwnerIdAndSha256Hash(Long ownerId, String sha256Hash);

    // --- YENİ EKLENEN METOT ---
    // Bu metot 'listeleme' (React UI) için gerekli
    List<ArtifactEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    @Transactional
    @Modifying
    void deleteByIdAndOwnerId(Long id, Long ownerId);
}
