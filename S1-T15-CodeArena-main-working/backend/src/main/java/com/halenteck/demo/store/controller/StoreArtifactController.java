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

    // --- 1. UPLOAD (YÜKLEME) ---
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "folderId", required = false) Long folderId,
                                    @RequestParam(value = "tags", required = false) List<String> tags,
                                    Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Yetkisiz erişim."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            String sha256 = sha256Hex(file.getBytes());
            if (artifactRepo.existsByOwnerIdAndSha256Hash(ownerId, sha256)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("duplicate", true, "message", "Bu içerikte bir dosya zaten yüklü."));
            }

            List<StoreArtifactEntity> existingVersions = artifactRepo.findByOwnerIdAndFilenameOrderByVersionNumberDesc(ownerId, file.getOriginalFilename());
            for (StoreArtifactEntity oldArtifact : existingVersions) {
                oldArtifact.setIsCurrentVersion(false);
                artifactRepo.save(oldArtifact);
            }

            int nextVersion = existingVersions.isEmpty() ? 1 : existingVersions.get(0).getVersionNumber() + 1;

            StoreArtifactEntity artifact = new StoreArtifactEntity();
            artifact.setOwnerId(ownerId);
            artifact.setFilename(file.getOriginalFilename());
            artifact.setMimeType(file.getContentType());
            artifact.setSizeBytes(file.getSize());
            artifact.setSha256Hash(sha256);
            artifact.setIsActive(true);
            artifact.setVersionNumber(nextVersion);
            artifact.setIsCurrentVersion(true);
            artifact.setCreatedAt(Instant.now());
            artifact.setUpdatedAt(Instant.now());
            artifact.setData(file.getBytes());

            if (folderId != null) {
                Optional<FolderEntity> folderOpt = folderRepo.findById(folderId);
                if (folderOpt.isPresent() && folderOpt.get().getOwnerId().equals(ownerId)) {
                    artifact.setFolder(folderOpt.get());
                }
            }

            if (tags != null && !tags.isEmpty()) {
                Set<TagEntity> tagEntities = new HashSet<>();
                for (String tagName : tags) {
                    String cleanTag = tagName.trim();
                    if (!cleanTag.isEmpty()) {
                        TagEntity tag = tagRepo.findByName(cleanTag).orElseGet(() -> {
                            TagEntity newTag = new TagEntity();
                            newTag.setName(cleanTag);
                            return tagRepo.save(newTag);
                        });
                        tagEntities.add(tag);
                    }
                }
                artifact.setTags(tagEntities);
            }

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

            StoreArtifactEntity savedArtifact = artifactRepo.save(artifact);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedArtifact);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 2. LISTELEME ---
    @GetMapping("/my-artifacts")
    public ResponseEntity<?> getMyArtifacts(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();
            List<StoreArtifactEntity> artifacts = artifactRepo.findByOwnerIdAndIsCurrentVersionTrueOrderByCreatedAtDesc(ownerId);
            return ResponseEntity.ok(artifacts);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 3. HISTORY ---
    @GetMapping("/history/{filename}")
    public ResponseEntity<?> getArtifactHistory(@PathVariable String filename, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();
            List<StoreArtifactEntity> history = artifactRepo.findByOwnerIdAndFilenameOrderByVersionNumberDesc(ownerId, filename);
            return ResponseEntity.ok(history);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 4. MAKE CURRENT ---
    @PutMapping("/{id}/make-current")
    public ResponseEntity<?> makeVersionCurrent(@PathVariable Long id, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            StoreArtifactEntity targetArtifact = artifactRepo.findById(id).orElseThrow(() -> new RuntimeException("Artifact not found"));
            if (!targetArtifact.getOwnerId().equals(ownerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Bu dosya size ait değil."));
            }

            List<StoreArtifactEntity> allVersions = artifactRepo.findByOwnerIdAndFilenameOrderByVersionNumberDesc(ownerId, targetArtifact.getFilename());
            for (StoreArtifactEntity artifact : allVersions) {
                artifact.setIsCurrentVersion(false);
                artifactRepo.save(artifact);
            }

            targetArtifact.setIsCurrentVersion(true);
            artifactRepo.save(targetArtifact);

            return ResponseEntity.ok(Map.of("message", "Version " + targetArtifact.getVersionNumber() + " is now current."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 5. DOWNLOAD ---
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadArtifact(@PathVariable Long id) {
        try {
            StoreArtifactEntity artifact = artifactRepo.findById(id).orElseThrow(() -> new RuntimeException("Artifact bulunamadı!"));
            byte[] fileContent = artifact.getData();
            if (fileContent == null || fileContent.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Dosya içeriği boş."));
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(artifact.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + artifact.getFilename() + "\"")
                    .body(fileContent);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 6. DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArtifact(@PathVariable Long id, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();
            artifactRepo.deleteByIdAndOwnerId(id, ownerId);
            return ResponseEntity.ok(Map.of("deleted", true, "id", id));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 7. UPDATE TAGS ---
    @PatchMapping("/{id}/tags")
    public ResponseEntity<?> updateArtifactTags(@PathVariable Long id, @RequestBody Map<String, Object> requestBody, Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Yetkisiz erişim."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            StoreArtifactEntity artifact = artifactRepo.findById(id).orElseThrow(() -> new RuntimeException("Artifact not found"));
            if (!artifact.getOwnerId().equals(ownerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Bu dosya size ait değil."));
            }

            @SuppressWarnings("unchecked")
            List<String> tagNames = (List<String>) requestBody.get("tags");
            if (tagNames == null) return ResponseEntity.badRequest().body(Map.of("error", "Tags listesi gerekli."));

            Set<TagEntity> newTags = new HashSet<>();
            for (String tagName : tagNames) {
                String cleanTag = tagName.trim();
                if (!cleanTag.isEmpty()) {
                    TagEntity tag = tagRepo.findByName(cleanTag).orElseGet(() -> {
                        TagEntity newTag = new TagEntity();
                        newTag.setName(cleanTag);
                        return tagRepo.save(newTag);
                    });
                    newTags.add(tag);
                }
            }
            artifact.setTags(newTags);
            artifact.setUpdatedAt(Instant.now());
            StoreArtifactEntity updatedArtifact = artifactRepo.save(artifact);

            return ResponseEntity.ok(Map.of("success", true, "message", "Tags başarıyla güncellendi.", "artifact", updatedArtifact));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 8. BULK UPLOAD ---
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUpload(@RequestParam("files") MultipartFile[] files,
                                        @RequestParam(value = "folderId", required = false) Long folderId,
                                        @RequestParam(value = "tags", required = false) List<String> tags,
                                        Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Yetkisiz erişim."));
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;
            int duplicateCount = 0;

            for (MultipartFile file : files) {
                try {
                    // Hash Hesapla
                    String sha256 = sha256Hex(file.getBytes());

                    // Duplicate kontrolü
                    if (artifactRepo.existsByOwnerIdAndSha256Hash(ownerId, sha256)) {
                        duplicateCount++;
                        results.add(Map.of(
                            "filename", file.getOriginalFilename(),
                            "status", "duplicate",
                            "message", "Bu içerikte bir dosya zaten yüklü."
                        ));
                        continue;
                    }

                    // Versiyonlama
                    List<StoreArtifactEntity> existingVersions = artifactRepo
                            .findByOwnerIdAndFilenameOrderByVersionNumberDesc(ownerId, file.getOriginalFilename());

                    for (StoreArtifactEntity oldArtifact : existingVersions) {
                        oldArtifact.setIsCurrentVersion(false);
                        artifactRepo.save(oldArtifact);
                    }

                    int nextVersion = existingVersions.isEmpty() ? 1 : existingVersions.get(0).getVersionNumber() + 1;

                    // Yeni artifact oluştur
                    StoreArtifactEntity artifact = new StoreArtifactEntity();
                    artifact.setOwnerId(ownerId);
                    artifact.setFilename(file.getOriginalFilename());
                    artifact.setMimeType(file.getContentType());
                    artifact.setSizeBytes(file.getSize());
                    artifact.setSha256Hash(sha256);
                    artifact.setIsActive(true);
                    artifact.setVersionNumber(nextVersion);
                    artifact.setIsCurrentVersion(true);
                    artifact.setCreatedAt(Instant.now());
                    artifact.setUpdatedAt(Instant.now());
                    artifact.setData(file.getBytes());

                    // Klasör
                    if (folderId != null) {
                        Optional<FolderEntity> folderOpt = folderRepo.findById(folderId);
                        if (folderOpt.isPresent() && folderOpt.get().getOwnerId().equals(ownerId)) {
                            artifact.setFolder(folderOpt.get());
                        }
                    }

                    // Etiketler
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

                    // Preview
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

                    // Kaydet
                    StoreArtifactEntity savedArtifact = artifactRepo.save(artifact);

                    successCount++;
                    results.add(Map.of(
                        "filename", file.getOriginalFilename(),
                        "status", "success",
                        "id", savedArtifact.getId(),
                        "version", nextVersion
                    ));

                } catch (Exception ex) {
                    failureCount++;
                    results.add(Map.of(
                        "filename", file.getOriginalFilename(),
                        "status", "error",
                        "message", ex.getMessage()
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                "total", files.length,
                "success", successCount,
                "failure", failureCount,
                "duplicate", duplicateCount,
                "results", results
            ));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 9. BULK IMPORT ---
    @PostMapping("/bulk-import")
    public ResponseEntity<?> bulkImport(@RequestBody Map<String, Object> importData,
                                        Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Yetkisiz erişim."));
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> artifacts = (List<Map<String, Object>>) importData.get("artifacts");

            if (artifacts == null || artifacts.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Artifact listesi boş olamaz."));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Map<String, Object> artifactData : artifacts) {
                try {
                    String filename = (String) artifactData.get("filename");
                    String mimeType = (String) artifactData.getOrDefault("mimeType", "application/octet-stream");
                    String base64Data = (String) artifactData.get("data");

                    if (filename == null || base64Data == null) {
                        failureCount++;
                        results.add(Map.of(
                            "filename", filename != null ? filename : "unknown",
                            "status", "error",
                            "message", "Filename ve data alanları zorunludur."
                        ));
                        continue;
                    }

                    // Base64'ten byte array'e çevir
                    byte[] fileData = Base64.getDecoder().decode(base64Data);

                    // Hash hesapla
                    String sha256 = sha256Hex(fileData);

                    // Duplicate kontrolü
                    if (artifactRepo.existsByOwnerIdAndSha256Hash(ownerId, sha256)) {
                        failureCount++;
                        results.add(Map.of(
                            "filename", filename,
                            "status", "duplicate",
                            "message", "Bu içerikte bir dosya zaten yüklü."
                        ));
                        continue;
                    }

                    // Versiyonlama
                    List<StoreArtifactEntity> existingVersions = artifactRepo
                            .findByOwnerIdAndFilenameOrderByVersionNumberDesc(ownerId, filename);

                    for (StoreArtifactEntity oldArtifact : existingVersions) {
                        oldArtifact.setIsCurrentVersion(false);
                        artifactRepo.save(oldArtifact);
                    }

                    int nextVersion = existingVersions.isEmpty() ? 1 : existingVersions.get(0).getVersionNumber() + 1;

                    // Yeni artifact oluştur
                    StoreArtifactEntity artifact = new StoreArtifactEntity();
                    artifact.setOwnerId(ownerId);
                    artifact.setFilename(filename);
                    artifact.setMimeType(mimeType);
                    artifact.setSizeBytes((long) fileData.length);
                    artifact.setSha256Hash(sha256);
                    artifact.setIsActive(true);
                    artifact.setVersionNumber(nextVersion);
                    artifact.setIsCurrentVersion(true);
                    artifact.setCreatedAt(Instant.now());
                    artifact.setUpdatedAt(Instant.now());
                    artifact.setData(fileData);

                    // Klasör
                    Object folderIdObj = artifactData.get("folderId");
                    if (folderIdObj != null) {
                        Long folderId = Long.valueOf(folderIdObj.toString());
                        Optional<FolderEntity> folderOpt = folderRepo.findById(folderId);
                        if (folderOpt.isPresent() && folderOpt.get().getOwnerId().equals(ownerId)) {
                            artifact.setFolder(folderOpt.get());
                        }
                    }

                    // Etiketler
                    @SuppressWarnings("unchecked")
                    List<String> tags = (List<String>) artifactData.get("tags");
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

                    // Preview
                    if (mimeType != null) {
                        boolean isText = mimeType.startsWith("text/") ||
                                         (mimeType.contains("json") && !mimeType.contains("openxmlformats")) ||
                                         (mimeType.contains("xml") && !mimeType.contains("openxmlformats")) ||
                                         mimeType.contains("javascript");

                        if (isText) {
                            String content = new String(fileData, StandardCharsets.UTF_8);
                            if (!content.contains("\u0000")) {
                                String preview = content.length() > 1000 ? content.substring(0, 1000) + "..." : content;
                                artifact.setPreviewText(preview);
                            }
                        }
                    }

                    // Kaydet
                    StoreArtifactEntity savedArtifact = artifactRepo.save(artifact);

                    successCount++;
                    results.add(Map.of(
                        "filename", filename,
                        "status", "success",
                        "id", savedArtifact.getId(),
                        "version", nextVersion
                    ));

                } catch (Exception ex) {
                    failureCount++;
                    results.add(Map.of(
                        "filename", artifactData.getOrDefault("filename", "unknown"),
                        "status", "error",
                        "message", ex.getMessage()
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                "total", artifacts.size(),
                "success", successCount,
                "failure", failureCount,
                "results", results
            ));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // --- 10. MOVE ARTIFACT (FIXLENDİ) ---
    @PutMapping("/{id}/move")
    public ResponseEntity<?> moveArtifact(@PathVariable Long id,
                                          @RequestBody Map<String, Object> requestBody, // Object alarak esneklik sağlandı
                                          Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Yetkisiz erişim."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            StoreArtifactEntity artifact = artifactRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artifact bulunamadı."));

            if (!artifact.getOwnerId().equals(ownerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Bu dosya size ait değil."));
            }

            // GÜVENLİ PARSE İŞLEMİ (Fix burada)
            Object folderIdObj = requestBody.get("folderId");
            Long folderId = null;
            
            if (folderIdObj != null) {
                String val = folderIdObj.toString();
                if (!val.isEmpty() && !val.equals("null")) {
                    try {
                        folderId = Long.valueOf(val);
                    } catch (NumberFormatException e) {
                        // Sayı değilse null kabul et
                    }
                }
            }

            if (folderId != null) {
                FolderEntity folder = folderRepo.findById(folderId)
                        .orElseThrow(() -> new RuntimeException("Hedef klasör bulunamadı."));
                
                if (!folder.getOwnerId().equals(ownerId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Hedef klasör size ait değil."));
                }
                artifact.setFolder(folder);
            } else {
                artifact.setFolder(null); // Klasörden çıkar
            }

            artifact.setUpdatedAt(Instant.now());
            StoreArtifactEntity updatedArtifact = artifactRepo.save(artifact);

            return ResponseEntity.ok(updatedArtifact);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}