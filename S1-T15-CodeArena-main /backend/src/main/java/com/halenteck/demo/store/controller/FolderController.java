package com.halenteck.demo.store.controller;

import com.halenteck.demo.security.CustomUserDetails;
import com.halenteck.demo.store.entity.FolderEntity;
import com.halenteck.demo.store.repository.FolderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderRepository folderRepository;

    // Constructor injection (Bağımlılık Enjeksiyonu)
    public FolderController(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    // 1. Klasör Oluştur (POST /api/folders)
    @PostMapping
    public ResponseEntity<?> createFolder(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            // Kullanıcı doğrulama
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Yetkisiz erişim."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();
            
            String name = request.get("name");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Klasör ismi boş olamaz."));
            }

            // Aynı isimde klasör var mı kontrol et
            // DÜZELTME: folderRepository örneği üzerinden çağrılıyor
            if (folderRepository.existsByOwnerIdAndName(ownerId, name)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Bu isimde bir klasör zaten var."));
            }

            FolderEntity folder = new FolderEntity();
            folder.setName(name);
            folder.setOwnerId(ownerId);
            
            // DÜZELTME: folderRepository örneği üzerinden kayıt yapılıyor
            FolderEntity savedFolder = folderRepository.save(folder);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFolder);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // 2. Klasörleri Listele (GET /api/folders)
    @GetMapping
    public ResponseEntity<?> getMyFolders(Authentication authentication) {
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Yetkisiz erişim."));
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long ownerId = userDetails.getId();

            // DÜZELTME: folderRepository örneği üzerinden listeleme yapılıyor
            List<FolderEntity> folders = folderRepository.findByOwnerId(ownerId);
            return ResponseEntity.ok(folders);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}