package com.halenteck.demo.store.repository;

import com.halenteck.demo.store.entity.StoreArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreArtifactRepo extends JpaRepository<StoreArtifactEntity, Long> {

    // Upload sırasında duplicate kontrolü için
    boolean existsByOwnerIdAndSha256Hash(Long ownerId, String sha256Hash);

    // --- LİSTELEME İÇİN ---
    // Sadece "GÜNCEL" (isCurrentVersion = true) olan dosyaları getirir.
    // Böylece aynı dosyanın eski versiyonları ana listede kirlilik yapmaz.
    List<StoreArtifactEntity> findByOwnerIdAndIsCurrentVersionTrueOrderByCreatedAtDesc(Long ownerId);

    // --- VERSİYONLAMA İÇİN (Hata veren kısım burasıydı) ---
    // Bir dosyanın (ismin) tüm geçmiş versiyonlarını bulur.
    // En yüksek versiyon numarası en üstte olacak şekilde sıralar.
    List<StoreArtifactEntity> findByOwnerIdAndFilenameOrderByVersionNumberDesc(Long ownerId, String filename);

    // Silme işlemi
    @Transactional
    @Modifying
    void deleteByIdAndOwnerId(Long id, Long ownerId);
}
