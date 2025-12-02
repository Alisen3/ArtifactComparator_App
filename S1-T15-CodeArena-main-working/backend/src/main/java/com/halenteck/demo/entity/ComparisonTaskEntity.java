// File: backend/src/main/java/com/halenteck/demo/entity/ComparisonTaskEntity.java
package com.halenteck.demo.entity;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artifact_a_id", nullable = false)
    private ArtifactEntity artifactA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artifact_b_id", nullable = false)
    private ArtifactEntity artifactB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_version_id")
    private StudyVersionEntity studyVersion;

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

    // --- NEW FIELDS FOR HIGHLIGHTS ---
    @Column(columnDefinition = "TEXT") // Use TEXT for potentially long JSON strings
    private String highlightDataA;

    @Column(columnDefinition = "TEXT")
    private String highlightDataB;
    // ---------------------------------

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public enum TaskStatus { PENDING, IN_PROGRESS, COMPLETED }

    public ComparisonTaskEntity() {}

    public ComparisonTaskEntity(StudyEntity study, UserEntity participant, ArtifactEntity artifactA, ArtifactEntity artifactB) {
        this.study = study;
        this.participant = participant;
        this.artifactA = artifactA;
        this.artifactB = artifactB;
        this.status = TaskStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // --- Getters and Setters ---
    // (Keep all existing getters/setters)

    public Long getId() { return id; }
    public StudyEntity getStudy() { return study; }
    public UserEntity getParticipant() { return participant; }
    public ArtifactEntity getArtifactA() { return artifactA; }
    public ArtifactEntity getArtifactB() { return artifactB; }
    public TaskStatus getStatus() { return status; }
    public String getAnnotations() { return annotations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public Double getClarityA() { return clarityA; }
    public Double getRelevanceA() { return relevanceA; }
    public Double getAccuracyA() { return accuracyA; }
    public String getCommentA() { return commentA; }

    public Double getClarityB() { return clarityB; }
    public Double getRelevanceB() { return relevanceB; }
    public Double getAccuracyB() { return accuracyB; }
    public String getCommentB() { return commentB; }

    // New Getters
    public String getHighlightDataA() { return highlightDataA; }
    public String getHighlightDataB() { return highlightDataB; }
    public StudyVersionEntity getStudyVersion() { return studyVersion; }

    public void setStatus(TaskStatus status) { this.status = status; }
    public void setAnnotations(String annotations) { this.annotations = annotations; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public void setClarityA(Double clarityA) { this.clarityA = clarityA; }
    public void setRelevanceA(Double relevanceA) { this.relevanceA = relevanceA; }
    public void setAccuracyA(Double accuracyA) { this.accuracyA = accuracyA; }
    public void setCommentA(String commentA) { this.commentA = commentA; }

    public void setClarityB(Double clarityB) { this.clarityB = clarityB; }
    public void setRelevanceB(Double relevanceB) { this.relevanceB = relevanceB; }
    public void setAccuracyB(Double accuracyB) { this.accuracyB = accuracyB; }
    public void setCommentB(String commentB) { this.commentB = commentB; }

    // New Setters
    public void setHighlightDataA(String highlightDataA) { this.highlightDataA = highlightDataA; }
    public void setHighlightDataB(String highlightDataB) { this.highlightDataB = highlightDataB; }
    public void setStudyVersion(StudyVersionEntity studyVersion) { this.studyVersion = studyVersion; }
}
