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

import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            Long ownerId = userDetails.getId();

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
            Long ownerId = userDetails.getId();

            List<ArtifactEntity> artifacts = repo.findByOwnerIdOrderByCreatedAtDesc(ownerId);
            return ResponseEntity.ok(artifacts);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- UPLOAD METODU (DB'ye Kayıt) ---
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Geçerli bir token bulunamadı veya kullanıcı detayı okunamadı."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            // Hash hesapla (Duplicate kontrolü için)
            String sha256 = sha256Hex(file.getBytes());

            if (repo.existsByOwnerIdAndSha256Hash(ownerId, sha256)) { 
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("duplicate", true, "message", "Same owner & hash exists"));
            }

            // Entity oluştur
            ArtifactEntity e = new ArtifactEntity();
            e.setOwnerId(ownerId);
            e.setFileName(file.getOriginalFilename());
            e.setMimeType(file.getContentType());
            e.setSizeBytes(file.getSize());
            e.setSha256Hash(sha256);
            e.setVersionNumber(1);
            e.setIsActive(true);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            
            // Dosya verisini BYTE ARRAY olarak set et
            e.setData(file.getBytes());

            // Not: storagePath ve storageUrl set etmiyoruz çünkü artık yoklar.
            // getStorageUrl() metodu entity içinde @Transient olduğu için otomatik hesaplanacak.

            ArtifactEntity savedEntity = repo.save(e); 

            return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- DOSYA İNDİRME ENDPOINT'İ (GET /{id}) ---
    // Artık diskten değil, veritabanından (getData) okuyor.
    @GetMapping("/{id}")
    public ResponseEntity<?> downloadArtifact(@PathVariable Long id) {
        try {
            ArtifactEntity artifact = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artifact not found!"));

            if (!artifact.getIsActive()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Artifact is not active"));
            }

            // Veritabanından byte array'i al
            byte[] fileContent = artifact.getData();
            
            if (fileContent == null || fileContent.length == 0) {
                 return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "File content is empty"));
            }
            
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

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("ok", true, "count", repo.count());
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}