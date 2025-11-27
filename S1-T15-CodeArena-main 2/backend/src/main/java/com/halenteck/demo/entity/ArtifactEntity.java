package com.halenteck.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.halenteck.demo.store.entity.FolderEntity; // Klasör Entity'si
import com.halenteck.demo.store.entity.TagEntity;    // Etiket Entity'si
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "store_artifacts") // Tablo ismini 'store_artifacts' olarak güncelledik
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

    // --- YENİ ÖZELLİKLER ---
    
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "is_current_version", nullable = false)
    private Boolean isCurrentVersion = true;

    // Dosya Verisi (Veritabanında BYTEA olarak)
    @JsonIgnore
    @Column(name = "data", nullable = false)
    private byte[] data;

    // Önizleme Metni
    @Column(name = "preview_text", length = 5000)
    private String previewText;

    // Klasör İlişkisi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    @JsonIgnore
    private FolderEntity folder;

    // Etiket İlişkisi
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "artifact_tags",
        joinColumns = @JoinColumn(name = "artifact_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags = new HashSet<>();

    // -----------------------

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // --- Sanal Alan (Frontend uyumluluğu için) ---
    @Transient
    @JsonProperty("storageUrl")
    public String getStorageUrl() {
        return "/api/artifacts/download/" + this.id;
    }

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    // Eski kodların bozulmaması için getFileName de ekleyelim
    public String getFileName() { return filename; } 

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public Boolean getIsCurrentVersion() { return isCurrentVersion; }
    public void setIsCurrentVersion(Boolean currentVersion) { isCurrentVersion = currentVersion; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getPreviewText() { return previewText; }
    public void setPreviewText(String previewText) { this.previewText = previewText; }

    public FolderEntity getFolder() { return folder; }
    public void setFolder(FolderEntity folder) { this.folder = folder; }

    public Set<TagEntity> getTags() { return tags; }
    public void setTags(Set<TagEntity> tags) { this.tags = tags; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}