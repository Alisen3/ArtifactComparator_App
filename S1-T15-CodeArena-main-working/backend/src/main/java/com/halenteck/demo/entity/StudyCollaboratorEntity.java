package com.halenteck.demo.entity;

import com.halenteck.demo.permission.StudyCollaboratorRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_collaborators",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_study_collaborator_user", columnNames = {"study_id", "collaborator_id"})
        })
public class StudyCollaboratorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private StudyEntity study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private UserEntity collaborator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyCollaboratorRole role;

    @Column(nullable = false)
    private String collaboratorEmail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public StudyCollaboratorEntity() {
    }

    public StudyCollaboratorEntity(StudyEntity study,
                                   UserEntity collaborator,
                                   StudyCollaboratorRole role) {
        this.study = study;
        this.collaborator = collaborator;
        this.role = role;
        this.collaboratorEmail = collaborator.getEmail();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public StudyEntity getStudy() {
        return study;
    }

    public UserEntity getCollaborator() {
        return collaborator;
    }

    public StudyCollaboratorRole getRole() {
        return role;
    }

    public String getCollaboratorEmail() {
        return collaboratorEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setRole(StudyCollaboratorRole role) {
        this.role = role;
    }

    public void setCollaboratorEmail(String collaboratorEmail) {
        this.collaboratorEmail = collaboratorEmail;
    }
}


