package com.halenteck.demo.controller;

import com.halenteck.demo.security.CustomUserDetails;
import com.halenteck.demo.entity.ArtifactEntity;
import com.halenteck.demo.repository.ArtifactRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List; 
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/artifacts")
public class ArtifactController {

    private final ArtifactRepository repo;

    public ArtifactController(ArtifactRepository repo) {
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

            List<ArtifactEntity> artifacts = repo.findByOwnerIdOrderByCreatedAtDesc(ownerId);
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

            ArtifactEntity e = new ArtifactEntity();
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

            ArtifactEntity savedEntity = repo.save(e); 

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

    // --- DOSYA İNDİRME ENDPOINT'İ (GET /{id}) ---
    @GetMapping("/{id}")
    public ResponseEntity<?> downloadArtifact(@PathVariable Long id) {
        try {
            ArtifactEntity artifact = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artifact not found!"));

            if (!artifact.getIsActive()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Artifact is not active"));
            }

            Path filePath = Paths.get(artifact.getStoragePath());
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "File not found on disk"));
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(artifact.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + artifact.getFileName() + "\"")
                    .body(fileContent);
                    
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
