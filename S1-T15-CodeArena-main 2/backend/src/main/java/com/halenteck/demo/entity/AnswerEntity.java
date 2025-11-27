// Dosya Yolu: demo/AnswerEntity.java
package com.halenteck.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "answers")
public class AnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Bu cevabın ait olduğu "Teslim" (Submission)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private QuizSubmissionEntity submission;

    // Bu cevabın ait olduğu "Soru"
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    // Cevap MULTIPLE_CHOICE ise: Katılımcının seçtiği şık
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "selected_option_id", nullable = true) // Sadece MC ise dolu
    private OptionEntity selectedOption;

    // Cevap SHORT_ANSWER ise: Katılımcının yazdığı metin
    @Column(length = 2000, nullable = true) // Sadece SA ise dolu
    private String answerText;


    public AnswerEntity() {
    }

    // Constructor (Çoktan Seçmeli Cevap için)
    public AnswerEntity(QuizSubmissionEntity submission, QuestionEntity question, OptionEntity selectedOption) {
        this.submission = submission;
        this.question = question;
        this.selectedOption = selectedOption;
        this.answerText = null;
    }

    // Constructor (Kısa Cevap için)
    public AnswerEntity(QuizSubmissionEntity submission, QuestionEntity question, String answerText) {
        this.submission = submission;
        this.question = question;
        this.selectedOption = null;
        this.answerText = answerText;
    }


    // --- Getters ---
    public Long getId() { return id; }
    public QuizSubmissionEntity getSubmission() { return submission; }
    public QuestionEntity getQuestion() { return question; }
    public OptionEntity getSelectedOption() { return selectedOption; }
    public String getAnswerText() { return answerText; }

    // --- Setters ---
    public void setSubmission(QuizSubmissionEntity submission) { this.submission = submission; }
    public void setQuestion(QuestionEntity question) { this.question = question; }
    public void setSelectedOption(OptionEntity selectedOption) { this.selectedOption = selectedOption; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}