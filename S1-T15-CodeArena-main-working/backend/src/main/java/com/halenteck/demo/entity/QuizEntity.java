// File: demo/entity/QuizEntity.java
package com.halenteck.demo.entity;

import com.halenteck.demo.QuizType; // Import newly created enum
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class QuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private Integer durationInMinutes; // Only for COMPETENCY_QUIZ

    // --- NEW FIELD ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizType type;
    // -----------------

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creator;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuestionEntity> questions = new ArrayList<>();

    public QuizEntity() {}

    // Updated Constructor
    public QuizEntity(String title, String description, UserEntity creator, Integer durationInMinutes, QuizType type) {
        this.title = title;
        this.description = description;
        this.creator = creator;
        this.durationInMinutes = durationInMinutes;
        this.type = type;
    }

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getDurationInMinutes() { return durationInMinutes; }
    public QuizType getType() { return type; } // Getter
    public UserEntity getCreator() { return creator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<QuestionEntity> getQuestions() { return questions; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDurationInMinutes(Integer durationInMinutes) { this.durationInMinutes = durationInMinutes; }
    public void setType(QuizType type) { this.type = type; } // Setter

    public void addQuestion(QuestionEntity question) {
        questions.add(question);
        question.setQuiz(this);
    }
}