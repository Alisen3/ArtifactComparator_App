// Dosya Yolu: src/main/java/com/halenteck/demo/store/controller/StoreArtifactController.java
package com.halenteck.demo.store.controller;

// Gerekli importları ekleyin
import com.halenteck.demo.security.CustomUserDetails;
import com.halenteck.demo.store.entity.StoreArtifactEntity;
import com.halenteck.demo.store.repository.StoreArtifactRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List; 

// ... (diğer importlar) ...
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// @Transactional import'u burada gereksiz, Repo seviyesinde var

@RestController
@RequestMapping("/api/store-artifacts")
public class StoreArtifactController {

    private final StoreArtifactRepo repo;

    public StoreArtifactController(StoreArtifactRepo repo) {
        this.repo = repo;
    }

    // --- SİLME ENDPOINT'İ (DELETE /{id}) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArtifact(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Geçerli bir token bulunamadı."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId(); // Token'dan 'Long' ID'yi al

            repo.deleteByIdAndOwnerId(id, ownerId); 
            return ResponseEntity.ok(Map.of("deleted", true, "id", id));
            
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- LİSTELEME ENDPOINT'İ (GET /my-artifacts) ---
    @GetMapping("/my-artifacts")
    public ResponseEntity<?> getMyArtifacts(Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Geçerli bir token bulunamadı."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId(); // Token'dan 'Long' ID'yi al

            List<StoreArtifactEntity> artifacts = repo.findByOwnerIdOrderByCreatedAtDesc(ownerId);
            return ResponseEntity.ok(artifacts); // Artifact listesini JSON olarak döndür

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }


    // --- UPLOAD METODU (DÖNÜŞ DEĞERİ GÜNCELLENDİ) ---
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Geçerli bir token bulunamadı veya kullanıcı detayı okunamadı."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            Long ownerId = userDetails.getId(); // Tip: Long

            String sha256 = sha256Hex(file.getBytes());

            if (repo.existsByOwnerIdAndSha256Hash(ownerId, sha256)) { 
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("duplicate", true, "message", "Same owner & hash exists"));
            }

            Path uploadDir = Paths.get("uploads");
            Files.createDirectories(uploadDir);
            String uniqueName = UUID.randomUUID() + "_" + Optional.ofNullable(file.getOriginalFilename()).orElse("file");
            Path target = uploadDir.resolve(uniqueName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            StoreArtifactEntity e = new StoreArtifactEntity();
            e.setOwnerId(ownerId); // Tip: Long
            e.setFileName(file.getOriginalFilename());
            e.setMimeType(file.getContentType());
            e.setSizeBytes(file.getSize());
            e.setSha256Hash(sha256);
            e.setVersionNumber(1);
            e.setStorageUrl("/uploads/" + uniqueName);
            e.setStoragePath(target.toAbsolutePath().toString());
            e.setIsActive(true);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());

            StoreArtifactEntity savedEntity = repo.save(e); 

            // --- DÜZELTME BURADA ---
            // React'in 'storageUrl' alabilmesi için
            // 'Map' yerine 'savedEntity'nin tamamını döndür.
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- MEVCUT PING METODUNUZ (Aynen kaldı) ---
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("ok", true, "count", repo.count());
    }

    // --- MEVCUT UTILS METODUNUZ (Aynen kaldı) ---
    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

