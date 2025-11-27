package com.halenteck.demo.store.controller;

import com.halenteck.demo.security.CustomUserDetails;
import com.halenteck.demo.store.entity.FolderEntity;
import com.halenteck.demo.store.entity.StoreArtifactEntity;
import com.halenteck.demo.store.entity.TagEntity;
import com.halenteck.demo.store.repository.FolderRepository;
import com.halenteck.demo.store.repository.StoreArtifactRepo;
import com.halenteck.demo.store.repository.TagRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/store-artifacts")
public class StoreArtifactController {

    private final StoreArtifactRepo artifactRepo;
    private final FolderRepository folderRepo;
    private final TagRepository tagRepo;

    public StoreArtifactController(StoreArtifactRepo artifactRepo, 
                                   FolderRepository folderRepo, 
                                   TagRepository tagRepo) {
        this.artifactRepo = artifactRepo;
        this.folderRepo = folderRepo;
        this.tagRepo = tagRepo;
    }

    // --- 1. UPLOAD (YÜKLEME) - Versiyonlama Destekli ---
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "folderId", required = false) Long folderId,
                                    @RequestParam(value = "tags", required = false) List<String> tags,
                                    Authentication authentication) {
        try {
            // Yetki Kontrolü
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Yetkisiz erişim."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            // 1. Hash Hesapla (Duplicate İçeriği Kontrol Et)
            String sha256 = sha256Hex(file.getBytes());
            if (artifactRepo.existsByOwnerIdAndSha256Hash(ownerId, sha256)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("duplicate", true, "message", "Bu içerikte bir dosya zaten yüklü."));
            }

            // 2. VERSİYONLAMA MANTIĞI
            // Aynı isimdeki eski dosyaları bul
            List<StoreArtifactEntity> existingVersions = artifactRepo.findByOwnerIdAndFilenameOrderByVersionNumberDesc(ownerId, file.getOriginalFilename());
            
            // Eski versiyonların "current" durumunu false yap (Arşivle)
            for (StoreArtifactEntity oldArtifact : existingVersions) {
                oldArtifact.setIsCurrentVersion(false);
                artifactRepo.save(oldArtifact);
            }

            // Yeni versiyon numarasını belirle (En yüksek + 1, yoksa 1)
            int nextVersion = existingVersions.isEmpty() ? 1 : existingVersions.get(0).getVersionNumber() + 1;

            // 3. Yeni Artifact Nesnesini Hazırla
            StoreArtifactEntity artifact = new StoreArtifactEntity();
            artifact.setOwnerId(ownerId);
            artifact.setFilename(file.getOriginalFilename());
            artifact.setMimeType(file.getContentType());
            artifact.setSizeBytes(file.getSize());
            artifact.setSha256Hash(sha256);
            artifact.setIsActive(true);
            
            // Versiyon Bilgileri
            artifact.setVersionNumber(nextVersion);
            artifact.setIsCurrentVersion(true); // Yeni yüklenen her zaman günceldir

            artifact.setCreatedAt(Instant.now());
            artifact.setUpdatedAt(Instant.now());
            
            // Dosya verisini set et (LOB/BYTEA)
            artifact.setData(file.getBytes());

            // 4. Klasörleme (Folder)
            if (folderId != null) {
                Optional<FolderEntity> folderOpt = folderRepo.findById(folderId);
                if (folderOpt.isPresent() && folderOpt.get().getOwnerId().equals(ownerId)) {
                    artifact.setFolder(folderOpt.get());
                }
            }

            // 5. Etiketleme (Tags)
            if (tags != null && !tags.isEmpty()) {
                Set<TagEntity> tagEntities = new HashSet<>();
                for (String tagName : tags) {
                    String cleanTag = tagName.trim();
                    if (!cleanTag.isEmpty()) {
                        TagEntity tag = tagRepo.findByName(cleanTag)
                                .orElseGet(() -> {
                                    TagEntity newTag = new TagEntity();
                                    newTag.setName(cleanTag);
                                    return tagRepo.save(newTag);
                                });
                        tagEntities.add(tag);
                    }
                }
                artifact.setTags(tagEntities);
            }

            // 6. Önizleme (Preview) Oluşturma
            String mimeType = file.getContentType();
            if (mimeType != null) {
                boolean isText = mimeType.startsWith("text/") || 
                                 (mimeType.contains("json") && !mimeType.contains("openxmlformats")) ||
                                 (mimeType.contains("xml") && !mimeType.contains("openxmlformats")) ||
                                 mimeType.contains("javascript");

                if (isText) {
                    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                    if (!content.contains("\u0000")) {
                        String preview = content.length() > 1000 ? content.substring(0, 1000) + "..." : content;
                        artifact.setPreviewText(preview);
                    }
                }
            }

            // 7. Kaydet
            StoreArtifactEntity savedArtifact = artifactRepo.save(artifact);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedArtifact);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 2. LISTELEME (Sadece GÜNCEL Versiyonlar) ---
    @GetMapping("/my-artifacts")
    public ResponseEntity<?> getMyArtifacts(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            // Sadece isCurrentVersion = true olanları getir
            List<StoreArtifactEntity> artifacts = artifactRepo.findByOwnerIdAndIsCurrentVersionTrueOrderByCreatedAtDesc(ownerId);
            return ResponseEntity.ok(artifacts);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 3. HISTORY (Bir dosyanın tüm geçmişi) ---
    @GetMapping("/history/{filename}")
    public ResponseEntity<?> getArtifactHistory(@PathVariable String filename, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            // İsmine göre tüm versiyonları getir
            List<StoreArtifactEntity> history = artifactRepo.findByOwnerIdAndFilenameOrderByVersionNumberDesc(ownerId, filename);
            return ResponseEntity.ok(history);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 4. MAKE CURRENT (Eski bir versiyonu güncel yap) ---
    @PutMapping("/{id}/make-current")
    public ResponseEntity<?> makeVersionCurrent(@PathVariable Long id, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            // Hedef artifact'ı bul
            StoreArtifactEntity targetArtifact = artifactRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artifact not found"));

            if (!targetArtifact.getOwnerId().equals(ownerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Bu dosya size ait değil."));
            }

            // Bu dosya adına sahip TÜM versiyonları bul
            List<StoreArtifactEntity> allVersions = artifactRepo.findByOwnerIdAndFilenameOrderByVersionNumberDesc(ownerId, targetArtifact.getFilename());

            // Hepsini false yap
            for (StoreArtifactEntity artifact : allVersions) {
                artifact.setIsCurrentVersion(false);
                artifactRepo.save(artifact);
            }

            // Hedeflenen versiyonu true yap
            targetArtifact.setIsCurrentVersion(true);
            artifactRepo.save(targetArtifact);

            return ResponseEntity.ok(Map.of("message", "Version " + targetArtifact.getVersionNumber() + " is now current."));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 5. DOWNLOAD (İNDİRME) ---
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadArtifact(@PathVariable Long id) {
        try {
            StoreArtifactEntity artifact = artifactRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artifact bulunamadı!"));

            byte[] fileContent = artifact.getData();
            if (fileContent == null || fileContent.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Dosya içeriği boş."));
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(artifact.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + artifact.getFilename() + "\"")
                    .body(fileContent);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 6. DELETE (SİLME) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArtifact(@PathVariable Long id, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            artifactRepo.deleteByIdAndOwnerId(id, ownerId);
            return ResponseEntity.ok(Map.of("deleted", true, "id", id));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- YARDIMCI METODLAR ---
    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}