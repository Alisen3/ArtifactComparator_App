// Dosya Yolu: demo/QuizSubmissionEntity.java
package com.halenteck.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_submissions")
public class QuizSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hangi kuis'in alındığı
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizEntity quiz;

    // Hangi katılımcının aldığı
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "participant_id", nullable = false)
    private UserEntity participant;

    // Issue #12: Zaman sınırını uygulamak için kuis'in ne zaman başlatıldığı
    @Column(nullable = false)
    private LocalDateTime startedAt;

    // Kuis'in ne zaman teslim edildiği
    private LocalDateTime submittedAt;

    // Issue #9 & #10: Otomatik notlandırmadan sonra hesaplanan puan
    // Başlangıçta (startedAt) null olacak, teslim edilince (submittedAt) hesaplanacak.
    private Double score;

    // Bu teslim işlemine ait tüm bireysel cevaplar
    @OneToMany(
            mappedBy = "submission",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<AnswerEntity> answers = new ArrayList<>();


    public QuizSubmissionEntity() {
    }

    public QuizSubmissionEntity(QuizEntity quiz, UserEntity participant) {
        this.quiz = quiz;
        this.participant = participant;
        this.startedAt = LocalDateTime.now(); // Başlatıldığı an
        this.score = null; // Henüz notlandırılmadı
    }

    // --- Getters ---
    public Long getId() { return id; }
    public QuizEntity getQuiz() { return quiz; }
    public UserEntity getParticipant() { return participant; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public Double getScore() { return score; }
    public List<AnswerEntity> getAnswers() { return answers; }

    // --- Setters ---
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public void setScore(Double score) { this.score = score; }

    // --- İlişki Yönetimi (Helper Methods) ---
    public void addAnswer(AnswerEntity answer) {
        answers.add(answer);
        answer.setSubmission(this);
    }
}