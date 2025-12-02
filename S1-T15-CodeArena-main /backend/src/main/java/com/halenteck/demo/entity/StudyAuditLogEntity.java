package com.halenteck.demo.entity;

import com.halenteck.demo.audit.StudyAuditAction;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_audit_logs")
public class StudyAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private StudyEntity study;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserEntity actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyAuditAction action;

    @Column(columnDefinition = "TEXT")
    private String detailsJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StudyAuditLogEntity() {
    }

    public StudyAuditLogEntity(StudyEntity study,
                               UserEntity actor,
                               StudyAuditAction action,
                               String detailsJson) {
        this.study = study;
        this.actor = actor;
        this.action = action;
        this.detailsJson = detailsJson;
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

    public UserEntity getActor() {
        return actor;
    }

    public StudyAuditAction getAction() {
        return action;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}


