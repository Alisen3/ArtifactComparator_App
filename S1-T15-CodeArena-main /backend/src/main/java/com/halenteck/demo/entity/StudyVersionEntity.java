package com.halenteck.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_versions")
public class StudyVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private StudyEntity study;

    @Column(nullable = false)
    private int versionNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String configJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    public StudyVersionEntity() {
    }

    public StudyVersionEntity(StudyEntity study,
                              int versionNumber,
                              String configJson,
                              LocalDateTime publishedAt) {
        this.study = study;
        this.versionNumber = versionNumber;
        this.configJson = configJson;
        this.publishedAt = publishedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public StudyEntity getStudy() {
        return study;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getConfigJson() {
        return configJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}


