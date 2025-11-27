// File: demo/entity/StudyEntity.java
package com.halenteck.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "studies")
public class StudyEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String title;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private boolean blinded = false;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creator;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "study_participants", joinColumns = @JoinColumn(name = "study_id"), inverseJoinColumns = @JoinColumn(name = "participant_id"))
    private Set<UserEntity> participants = new HashSet<>();

    // Existing: Technical Quiz
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competency_quiz_id", nullable = true)
    private QuizEntity competencyQuiz;

    // --- NEW: Background Questionnaire (Issue #5) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_questionnaire_id", nullable = true)
    private QuizEntity backgroundQuestionnaire;
    // ------------------------------------------------

    public StudyEntity() {}

    public StudyEntity(String title, String description, boolean blinded, UserEntity creator) {
        this.title = title;
        this.description = description;
        this.blinded = blinded;
        this.creator = creator;
    }

    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isBlinded() { return blinded; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UserEntity getCreator() { return creator; }
    public Set<UserEntity> getParticipants() { return participants; }
    public QuizEntity getCompetencyQuiz() { return competencyQuiz; }
    public QuizEntity getBackgroundQuestionnaire() { return backgroundQuestionnaire; } // New Getter

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setBlinded(boolean blinded) { this.blinded = blinded; }
    public void setCompetencyQuiz(QuizEntity competencyQuiz) { this.competencyQuiz = competencyQuiz; }
    public void setBackgroundQuestionnaire(QuizEntity backgroundQuestionnaire) { this.backgroundQuestionnaire = backgroundQuestionnaire; } // New Setter

    public void addParticipant(UserEntity participant) { this.participants.add(participant); participant.getStudies().add(this); }
    public void removeParticipant(UserEntity participant) { this.participants.remove(participant); participant.getStudies().remove(this); }
}