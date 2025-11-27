package com.halenteck.demo.entity;

import com.halenteck.demo.store.entity.StoreArtifactEntity; // YENİ IMPORT
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comparison_tasks")
public class ComparisonTaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "study_id", nullable = false)
    private StudyEntity study;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "participant_id", nullable = false)
    private UserEntity participant;

    // --- DEĞİŞİKLİK: StoreArtifactEntity'ye bağlandı ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artifact_a_id", nullable = false)
    private StoreArtifactEntity artifactA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artifact_b_id", nullable = false)
    private StoreArtifactEntity artifactB;
    // ---------------------------------------------------

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(length = 5000)
    private String annotations;

    // Ratings A
    private Double clarityA;
    private Double relevanceA;
    private Double accuracyA;
    @Column(length = 2000)
    private String commentA;

    // Ratings B
    private Double clarityB;
    private Double relevanceB;
    private Double accuracyB;
    @Column(length = 2000)
    private String commentB;

    // Highlights
    @Column(columnDefinition = "TEXT")
    private String highlightDataA;

    @Column(columnDefinition = "TEXT")
    private String highlightDataB;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public enum TaskStatus { PENDING, IN_PROGRESS, COMPLETED }

    public ComparisonTaskEntity() {}

    // Constructor Güncellendi
    public ComparisonTaskEntity(StudyEntity study, UserEntity participant, StoreArtifactEntity artifactA, StoreArtifactEntity artifactB) {
        this.study = study;
        this.participant = participant;
        this.artifactA = artifactA;
        this.artifactB = artifactB;
        this.status = TaskStatus.PENDING;
    }

    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // --- Getters / Setters (Tipler Güncellendi) ---
    public Long getId() { return id; }
    public StudyEntity getStudy() { return study; }
    public UserEntity getParticipant() { return participant; }
    
    public StoreArtifactEntity getArtifactA() { return artifactA; }
    public void setArtifactA(StoreArtifactEntity artifactA) { this.artifactA = artifactA; }

    public StoreArtifactEntity getArtifactB() { return artifactB; }
    public void setArtifactB(StoreArtifactEntity artifactB) { this.artifactB = artifactB; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public String getAnnotations() { return annotations; }
    public void setAnnotations(String annotations) { this.annotations = annotations; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Double getClarityA() { return clarityA; }
    public void setClarityA(Double clarityA) { this.clarityA = clarityA; }
    public Double getRelevanceA() { return relevanceA; }
    public void setRelevanceA(Double relevanceA) { this.relevanceA = relevanceA; }
    public Double getAccuracyA() { return accuracyA; }
    public void setAccuracyA(Double accuracyA) { this.accuracyA = accuracyA; }
    public String getCommentA() { return commentA; }
    public void setCommentA(String commentA) { this.commentA = commentA; }

    public Double getClarityB() { return clarityB; }
    public void setClarityB(Double clarityB) { this.clarityB = clarityB; }
    public Double getRelevanceB() { return relevanceB; }
    public void setRelevanceB(Double relevanceB) { this.relevanceB = relevanceB; }
    public Double getAccuracyB() { return accuracyB; }
    public void setAccuracyB(Double accuracyB) { this.accuracyB = accuracyB; }
    public String getCommentB() { return commentB; }
    public void setCommentB(String commentB) { this.commentB = commentB; }

    public String getHighlightDataA() { return highlightDataA; }
    public void setHighlightDataA(String highlightDataA) { this.highlightDataA = highlightDataA; }
    public String getHighlightDataB() { return highlightDataB; }
    public void setHighlightDataB(String highlightDataB) { this.highlightDataB = highlightDataB; }
}