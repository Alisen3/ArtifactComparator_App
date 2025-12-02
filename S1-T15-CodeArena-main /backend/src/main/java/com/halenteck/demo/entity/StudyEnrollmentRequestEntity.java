package com.halenteck.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_enrollment_requests")
public class StudyEnrollmentRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private StudyEntity study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private UserEntity participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyEnrollmentStatus status = StudyEnrollmentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_id")
    private StudyInviteEntity invite;

    @Column(columnDefinition = "TEXT")
    private String evaluationSnapshotJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private UserEntity reviewedBy;

    @Column(length = 500)
    private String reviewerNote;

    public StudyEnrollmentRequestEntity() {
    }

    public StudyEnrollmentRequestEntity(StudyEntity study,
                                        UserEntity participant,
                                        StudyInviteEntity invite,
                                        String evaluationSnapshotJson) {
        this.study = study;
        this.participant = participant;
        this.invite = invite;
        this.evaluationSnapshotJson = evaluationSnapshotJson;
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

    public UserEntity getParticipant() {
        return participant;
    }

    public StudyEnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(StudyEnrollmentStatus status) {
        this.status = status;
    }

    public StudyInviteEntity getInvite() {
        return invite;
    }

    public String getEvaluationSnapshotJson() {
        return evaluationSnapshotJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UserEntity getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UserEntity reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewerNote() {
        return reviewerNote;
    }

    public void setReviewerNote(String reviewerNote) {
        this.reviewerNote = reviewerNote;
    }
}

