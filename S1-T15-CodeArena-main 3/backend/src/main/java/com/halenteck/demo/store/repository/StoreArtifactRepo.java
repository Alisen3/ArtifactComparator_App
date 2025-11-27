// Dosya Yolu: src/main/java/com/halenteck/demo/store/repository/StoreArtifactRepo.java
package com.halenteck.demo.store.repository;

import com.halenteck.demo.store.entity.StoreArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface StoreArtifactRepo extends JpaRepository<StoreArtifactEntity, Long> { 
    
    // Bu metot 'upload' için gerekli
    boolean existsByOwnerIdAndSha256Hash(Long ownerId, String sha256Hash);

    // --- YENİ EKLENEN METOT ---
    // Bu metot 'listeleme' (React UI) için gerekli
    List<StoreArtifactEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    @Transactional
    @Modifying
    void deleteByIdAndOwnerId(Long id, Long ownerId);
}

