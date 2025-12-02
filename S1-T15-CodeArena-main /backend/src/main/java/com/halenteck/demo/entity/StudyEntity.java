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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyStatus status = StudyStatus.DRAFT;

    private LocalDateTime accessWindowStart;
    private LocalDateTime accessWindowEnd;

    @Column(nullable = false)
    private int nextVersionNumber = 1;

    private Integer latestPublishedVersionNumber;

    @Column(nullable = false)
    private boolean hasUnpublishedChanges = true;

    private Long sourceTemplateId;
    private String sourceTemplateName;
    private Long clonedFromStudyId;
    @Column(length = 500)
    private String provenanceNote;

    @Column(columnDefinition = "TEXT")
    private String eligibilityRulesJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EligibilityApprovalMode eligibilityApprovalMode = EligibilityApprovalMode.AUTO;

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
    public StudyStatus getStatus() { return status; }
    public LocalDateTime getAccessWindowStart() { return accessWindowStart; }
    public LocalDateTime getAccessWindowEnd() { return accessWindowEnd; }
    public int getNextVersionNumber() { return nextVersionNumber; }
    public Integer getLatestPublishedVersionNumber() { return latestPublishedVersionNumber; }
    public boolean isHasUnpublishedChanges() { return hasUnpublishedChanges; }
    public Long getSourceTemplateId() { return sourceTemplateId; }
    public String getSourceTemplateName() { return sourceTemplateName; }
    public Long getClonedFromStudyId() { return clonedFromStudyId; }
    public String getProvenanceNote() { return provenanceNote; }
    public String getEligibilityRulesJson() { return eligibilityRulesJson; }
    public EligibilityApprovalMode getEligibilityApprovalMode() { return eligibilityApprovalMode; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setBlinded(boolean blinded) { this.blinded = blinded; }
    public void setCompetencyQuiz(QuizEntity competencyQuiz) { this.competencyQuiz = competencyQuiz; }
    public void setBackgroundQuestionnaire(QuizEntity backgroundQuestionnaire) { this.backgroundQuestionnaire = backgroundQuestionnaire; } // New Setter
    public void setStatus(StudyStatus status) { this.status = status; }
    public void setAccessWindowStart(LocalDateTime accessWindowStart) { this.accessWindowStart = accessWindowStart; }
    public void setAccessWindowEnd(LocalDateTime accessWindowEnd) { this.accessWindowEnd = accessWindowEnd; }
    public void setNextVersionNumber(int nextVersionNumber) { this.nextVersionNumber = nextVersionNumber; }
    public void setLatestPublishedVersionNumber(Integer latestPublishedVersionNumber) { this.latestPublishedVersionNumber = latestPublishedVersionNumber; }
    public void setHasUnpublishedChanges(boolean hasUnpublishedChanges) { this.hasUnpublishedChanges = hasUnpublishedChanges; }
    public void setSourceTemplateId(Long sourceTemplateId) { this.sourceTemplateId = sourceTemplateId; }
    public void setSourceTemplateName(String sourceTemplateName) { this.sourceTemplateName = sourceTemplateName; }
    public void setClonedFromStudyId(Long clonedFromStudyId) { this.clonedFromStudyId = clonedFromStudyId; }
    public void setProvenanceNote(String provenanceNote) { this.provenanceNote = provenanceNote; }
    public void setEligibilityRulesJson(String eligibilityRulesJson) { this.eligibilityRulesJson = eligibilityRulesJson; }
    public void setEligibilityApprovalMode(EligibilityApprovalMode eligibilityApprovalMode) { this.eligibilityApprovalMode = eligibilityApprovalMode; }

    public void addParticipant(UserEntity participant) { this.participants.add(participant); participant.getStudies().add(this); }
    public void removeParticipant(UserEntity participant) { this.participants.remove(participant); participant.getStudies().remove(this); }
}