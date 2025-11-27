// Dosya Yolu: demo/QuizService.java
package com.halenteck.demo.service;
import com.halenteck.demo.QuizType;

import com.halenteck.demo.QuestionType;
import com.halenteck.demo.dto.*;
import com.halenteck.demo.entity.*;
import com.halenteck.demo.repository.*;
import org.springframework.security.access.AccessDeniedException; // Yeni eklendi
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final AnswerRepository answerRepository;


    public QuizService(QuizRepository quizRepository,
                       UserRepository userRepository,
                       StudyRepository studyRepository,
                       QuizSubmissionRepository submissionRepository,
                       QuestionRepository questionRepository,
                       OptionRepository optionRepository,
                       AnswerRepository answerRepository) {
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.studyRepository = studyRepository;
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.answerRepository = answerRepository;
    }

    // --- (METOD 1 & 2 DEĞİŞMEDİ) ---

    @Transactional
    public QuizEntity createQuiz(CreateQuizRequest request, Principal principal) {
        // (Mevcut createQuiz metodunuzun içeriği burada)
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));

        QuizType type = request.type() != null ? request.type() : QuizType.COMPETENCY_QUIZ;

        QuizEntity newQuiz = new QuizEntity(
                request.title(),
                request.description(),
                creator,
                request.durationInMinutes(),
                type // <--- Pass type
        );

        for (CreateQuestionDTO questionDTO : request.questions()) {
            QuestionEntity newQuestion = new QuestionEntity(
                    newQuiz,
                    questionDTO.questionText(),
                    questionDTO.questionType()
            );

            if (questionDTO.questionType() == QuestionType.MULTIPLE_CHOICE && questionDTO.options() != null) {
                for (CreateOptionDTO optionDTO : questionDTO.options()) {
                    OptionEntity newOption = new OptionEntity(
                            newQuestion,
                            optionDTO.optionText(),
                            optionDTO.isCorrect()
                    );
                    newQuestion.addOption(newOption);
                }
            }
            newQuiz.addQuestion(newQuestion);
        }
        return quizRepository.save(newQuiz);
    }

    @Transactional(readOnly = true)
    public List<QuizSummaryDTO> findQuizzesByCreator(Principal principal) {
        // (Mevcut findQuizzesByCreator metodunuzun içeriği burada)
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));

        List<QuizEntity> quizzes = quizRepository.findByCreator(creator);

        return quizzes.stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    // --- (METOD 3 & 4 DEĞİŞMEDİ) ---

    @Transactional
    public QuizTakeDTO getQuizForParticipant(Long studyId, Principal principal) {
        // (Mevcut getQuizForParticipant metodunuzun içeriği burada)
        UserEntity participant = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        StudyEntity study = studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("Study not found"));

        QuizEntity quiz = study.getCompetencyQuiz();
        if (quiz == null) {
            throw new RuntimeException("No competency quiz assigned to this study.");
        }

        QuizSubmissionEntity submission = submissionRepository
                .findByQuizAndParticipant(quiz, participant)
                .orElseGet(() -> {
                    QuizSubmissionEntity newSubmission = new QuizSubmissionEntity(quiz, participant);
                    return submissionRepository.save(newSubmission);
                });

        if (submission.getSubmittedAt() != null) {
            throw new RuntimeException("You have already completed this quiz.");
        }

        return convertToQuizTakeDTO(quiz);
    }

    @Transactional
    public Map<String, Object> submitQuiz(Long studyId, QuizSubmitRequest request, Principal principal) {
        // (Mevcut submitQuiz ve Otomatik Notlandırma metodunuzun içeriği burada)
        UserEntity participant = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        StudyEntity study = studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("Study not found"));

        QuizEntity quiz = study.getCompetencyQuiz();
        if (quiz == null) {
            throw new RuntimeException("No quiz assigned to this study.");
        }

        QuizSubmissionEntity submission = submissionRepository
                .findByQuizAndParticipant(quiz, participant)
                .orElseThrow(() -> new RuntimeException("Quiz not started. Please 'GET' the quiz first."));

        if (submission.getSubmittedAt() != null) {
            throw new RuntimeException("Quiz already submitted.");
        }

        Integer duration = quiz.getDurationInMinutes();
        if (duration != null) {
            LocalDateTime deadline = submission.getStartedAt().plusMinutes(duration);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new RuntimeException("Time limit exceeded. Submission rejected.");
            }
        }

        double totalCorrect = 0;
        int totalGradableQuestions = 0;

        for (AnswerSubmitDTO answerDTO : request.answers()) {
            QuestionEntity question = questionRepository.findById(answerDTO.questionId())
                    .orElseThrow(() -> new RuntimeException("Question not found: " + answerDTO.questionId()));

            AnswerEntity answerEntity;

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                totalGradableQuestions++;
                OptionEntity selectedOption = optionRepository.findById(answerDTO.selectedOptionId())
                        .orElseThrow(() -> new RuntimeException("Option not found: " + answerDTO.selectedOptionId()));

                if (selectedOption.isCorrect()) {
                    totalCorrect++;
                }
                answerEntity = new AnswerEntity(submission, question, selectedOption);

            } else {
                answerEntity = new AnswerEntity(submission, question, answerDTO.answerText());
            }
            answerRepository.save(answerEntity);
        }

        double finalScore = 0.0;
        if (totalGradableQuestions > 0) {
            finalScore = (totalCorrect / totalGradableQuestions) * 100.0;
        }

        submission.setScore(finalScore);
        submission.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        return Map.of(
                "message", "Quiz submitted successfully!",
                "score", finalScore,
                "correctAnswers", (int)totalCorrect,
                "totalQuestions", totalGradableQuestions
        );
    }


    // --- --- YENİ EKLENEN METOD (ARAŞTIRMACI İÇİN) --- ---

    /**
     * Metod 5: Bir Çalışmadaki Kuis Puanlarını Getir (Issue #9, #10)
     * Araştırmacının, çalışmasına atanmış kuis'i tamamlayan
     * tüm katılımcıların puanlarını görmesini sağlar.
     */
    @Transactional(readOnly = true)
    public List<SubmissionSummaryDTO> getSubmissionsForStudy(Long studyId, Principal principal) {
        // 1. Araştırmacıyı ve Çalışmayı bul
        UserEntity creator = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        StudyEntity study = studyRepository.findById(studyId)
                .orElseThrow(() -> new RuntimeException("Study not found"));

        // 2. Güvenlik Kontrolü: Bu çalışmanın sahibi mi?
        if (!study.getCreator().getId().equals(creator.getId())) {
            throw new AccessDeniedException("You are not the creator of this study.");
        }

        // 3. Çalışmaya atanmış Kuis'i bul
        QuizEntity quiz = study.getCompetencyQuiz();
        if (quiz == null) {
            // Çalışmaya kuis atanmamışsa boş liste döndür
            return List.of();
        }

        // 4. O Kuis'e ait tüm 'Submission'ları (Teslimleri) bul
        List<QuizSubmissionEntity> submissions = submissionRepository.findByQuiz(quiz);

        // 5. Entity listesini DTO listesine dönüştür (Stream API kullanarak)
        return submissions.stream()
                .filter(sub -> sub.getSubmittedAt() != null) // Sadece tamamlanmış olanları al
                .map(this::convertToSubmissionSummaryDTO) // DTO'ya çevir
                .collect(Collectors.toList());
    }


    // --- --- YARDIMCI DÖNÜŞTÜRME METODLARI --- ---

    private QuizSummaryDTO convertToSummaryDTO(QuizEntity quiz) {
        // (Mevcut convertToSummaryDTO metodunuzun içeriği burada)
        return new QuizSummaryDTO(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getDurationInMinutes(),
                quiz.getCreatedAt(),
                quiz.getQuestions().size()
        );
    }

    private QuizTakeDTO convertToQuizTakeDTO(QuizEntity quiz) {
        // (Mevcut convertToQuizTakeDTO metodunuzun içeriği burada)
        List<QuizQuestionDTO> questionDTOs = quiz.getQuestions().stream()
                .map(question -> {
                    List<QuizOptionDTO> optionDTOs = question.getOptions().stream()
                            .map(option -> new QuizOptionDTO(
                                    option.getId(),
                                    option.getOptionText()
                            ))
                            .collect(Collectors.toList());

                    return new QuizQuestionDTO(
                            question.getId(),
                            question.getQuestionText(),
                            question.getQuestionType(),
                            optionDTOs
                    );
                })
                .collect(Collectors.toList());

        return new QuizTakeDTO(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getDurationInMinutes(),
                questionDTOs
        );
    }

    /**
     * YENİ Yardımcı Metod: Bir QuizSubmissionEntity'yi bir SubmissionSummaryDTO'ya dönüştürür.
     */
    private SubmissionSummaryDTO convertToSubmissionSummaryDTO(QuizSubmissionEntity submission) {
        UserEntity participant = submission.getParticipant();
        return new SubmissionSummaryDTO(
                submission.getId(),
                participant.getId(),
                participant.getName(),
                submission.getScore(),
                submission.getSubmittedAt()
        );
    }
}