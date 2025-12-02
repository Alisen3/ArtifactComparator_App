package com.halenteck.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "study_invites", indexes = {
        @Index(name = "idx_invite_token", columnList = "token", unique = true)
})
public class StudyInviteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private StudyEntity study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private UserEntity invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user_id")
    private UserEntity invitedUser;

    @Column(nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyInviteStatus status = StudyInviteStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyInviteDeliveryMethod deliveryMethod = StudyInviteDeliveryMethod.EMAIL;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_id")
    private UserEntity acceptedBy;

    @Column(length = 320)
    private String email;

    public StudyInviteEntity() {
    }

    public StudyInviteEntity(StudyEntity study,
                             UserEntity invitedBy,
                             String email,
                             StudyInviteDeliveryMethod deliveryMethod,
                             LocalDateTime expiresAt) {
        this.study = study;
        this.invitedBy = invitedBy;
        this.email = email;
        this.deliveryMethod = deliveryMethod;
        this.expiresAt = expiresAt;
        this.token = UUID.randomUUID().toString().replace("-", "");
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

    public UserEntity getInvitedBy() {
        return invitedBy;
    }

    public UserEntity getInvitedUser() {
        return invitedUser;
    }

    public void setInvitedUser(UserEntity invitedUser) {
        this.invitedUser = invitedUser;
    }

    public String getToken() {
        return token;
    }

    public StudyInviteStatus getStatus() {
        return status;
    }

    public void setStatus(StudyInviteStatus status) {
        this.status = status;
    }

    public StudyInviteDeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public UserEntity getAcceptedBy() {
        return acceptedBy;
    }

    public void setAcceptedBy(UserEntity acceptedBy) {
        this.acceptedBy = acceptedBy;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

