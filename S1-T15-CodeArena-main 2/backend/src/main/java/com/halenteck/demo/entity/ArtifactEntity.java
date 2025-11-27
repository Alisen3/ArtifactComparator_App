package com.halenteck.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "store_artifacts")
public class ArtifactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "sha256_hash", nullable = false)
    private String sha256Hash;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    // --- DEĞİŞİKLİK: storage_path SİLİNDİ, data EKLENDİ ---
    // (Diğer entity ile uyumlu olması için)
    @JsonIgnore
    @Column(name = "data", nullable = false)
    private byte[] data;
    // -----------------------------------------------------

    // Bu entity'de Folder ve Tag ilişkilerini kullanmıyor olabilirsiniz,
    // ama veritabanı hatası almamak için temel sütunların eşleşmesi şart.
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // --- storageUrl ARTIK SANAL (DB'de yok) ---
    @Transient
    @JsonProperty("storageUrl")
    public String getStorageUrl() {
        if (this.id == null) return null;
        // Not: StoreArtifactController kullanıldığı için buradaki path çok önemli değil
        // ama null dönmemesi için id bazlı bir şey dönebiliriz.
        return "/api/store-artifacts/download/" + this.id;
    }

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
    
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}