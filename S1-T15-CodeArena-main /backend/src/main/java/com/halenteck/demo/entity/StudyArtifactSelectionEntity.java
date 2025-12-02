package com.halenteck.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_artifacts",
        uniqueConstraints = @UniqueConstraint(name = "uk_study_artifact", columnNames = {"study_id", "artifact_id"}))
public class StudyArtifactSelectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private StudyEntity study;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false)
    private ArtifactEntity artifact;

    @Column(length = 255)
    private String alias;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StudyArtifactSelectionEntity() {
    }

    public StudyArtifactSelectionEntity(StudyEntity study, ArtifactEntity artifact, String alias) {
        this.study = study;
        this.artifact = artifact;
        this.alias = alias;
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

    public ArtifactEntity getArtifact() {
        return artifact;
    }

    public String getAlias() {
        return alias;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}

