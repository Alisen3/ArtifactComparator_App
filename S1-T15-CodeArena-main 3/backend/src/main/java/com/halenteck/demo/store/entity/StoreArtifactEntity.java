// Dosya Yolu: src/main/java/com/halenteck/demo/store/entity/StoreArtifactEntity.java
package com.halenteck.demo.store.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "store_artifacts")
public class StoreArtifactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Otomatik artan Long ID
    @Column(name = "id", nullable = false)
    private Long id; // Tip: Long

    @Column(name = "owner_id", nullable = false)
    private Long ownerId; // Tip: Long

    @Column(name = "filename", nullable = false)
    private String filename;
    @Column(name = "mime_type", nullable = false)
    private String mimeType;
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;
    @Column(name = "sha256_hash", nullable = false)
    private String sha256Hash;
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    @Column(name = "storage_url", nullable = false)
    private String storageUrl;
    @Column(name = "storage_path", nullable = false)
    private String storagePath;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // --- Getters / Setters ---
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    
    public String getFileName() { return filename; }
    public void setFileName(String filename) { this.filename = filename; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public String getStorageUrl() { return storageUrl; }
    public void setStorageUrl(String storageUrl) { this.storageUrl = storageUrl; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

