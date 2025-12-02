// Dosya Yolu: demo/service/ParticipantService.java
package com.halenteck.demo.service;

import com.halenteck.demo.UserRole;
import com.halenteck.demo.dto.ParticipantFilterRequestDTO;
import com.halenteck.demo.dto.ParticipantWithScoresDTO;
import com.halenteck.demo.entity.*;
import com.halenteck.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ParticipantService {

    private final UserRepository userRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final AnswerRepository answerRepository;

    public ParticipantService(UserRepository userRepository,
                             QuizSubmissionRepository submissionRepository,
                             AnswerRepository answerRepository) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
    }

    /**
     * Get all participants with their quiz scores and questionnaire answers
     */
    @Transactional(readOnly = true)
    public List<ParticipantWithScoresDTO> getAllParticipantsWithScores() {
        List<UserEntity> participants = userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.PARTICIPANT)
                .collect(Collectors.toList());

        return participants.stream()
                .map(this::convertToParticipantWithScoresDTO)
                .collect(Collectors.toList());
    }

    /**
     * Filter participants based on criteria
     */
    @Transactional(readOnly = true)
    public List<ParticipantWithScoresDTO> filterParticipants(ParticipantFilterRequestDTO filterRequest) {
        List<ParticipantWithScoresDTO> allParticipants = getAllParticipantsWithScores();

        return allParticipants.stream()
                .filter(participant -> matchesFilters(participant, filterRequest))
                .collect(Collectors.toList());
    }

    /**
     * Convert UserEntity to ParticipantWithScoresDTO
     */
    private ParticipantWithScoresDTO convertToParticipantWithScoresDTO(UserEntity user) {
        // Get all quiz submissions for this participant
        List<QuizSubmissionEntity> submissions = submissionRepository.findByParticipant(user);
        
        // Build quiz scores map (quizId -> score)
        Map<Long, Double> quizScores = new HashMap<>();
        Map<Long, String> questionnaireAnswers = new HashMap<>();

        for (QuizSubmissionEntity submission : submissions) {
            if (submission.getSubmittedAt() != null && submission.getScore() != null) {
                Long quizId = submission.getQuiz().getId();
                quizScores.put(quizId, submission.getScore());
            }

            // Get answers for this submission
            List<AnswerEntity> answers = answerRepository.findBySubmission(submission);
            for (AnswerEntity answer : answers) {
                Long questionId = answer.getQuestion().getId();
                String answerText = null;

                if (answer.getSelectedOption() != null) {
                    // Multiple choice answer
                    answerText = answer.getSelectedOption().getOptionText();
                } else if (answer.getAnswerText() != null) {
                    // Short answer
                    answerText = answer.getAnswerText();
                }

                if (answerText != null) {
                    // If multiple answers for same question, concatenate them
                    if (questionnaireAnswers.containsKey(questionId)) {
                        questionnaireAnswers.put(questionId, 
                            questionnaireAnswers.get(questionId) + "; " + answerText);
                    } else {
                        questionnaireAnswers.put(questionId, answerText);
                    }
                }
            }
        }

        return new ParticipantWithScoresDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getSkills(),
                user.getYearsOfExperience(),
                quizScores,
                questionnaireAnswers
        );
    }

    /**
     * Check if a participant matches all filter criteria
     */
    private boolean matchesFilters(ParticipantWithScoresDTO participant, ParticipantFilterRequestDTO filters) {
        // Filter by quiz scores
        if (filters.minQuizScores() != null && !filters.minQuizScores().isEmpty()) {
            for (Map.Entry<Long, Double> entry : filters.minQuizScores().entrySet()) {
                Long quizId = entry.getKey();
                Double minScore = entry.getValue();
                Double participantScore = participant.quizScores().get(quizId);
                
                if (participantScore == null || participantScore < minScore) {
                    return false;
                }
            }
        }

        // Filter by questionnaire answers
        if (filters.questionnaireAnswers() != null && !filters.questionnaireAnswers().isEmpty()) {
            for (Map.Entry<Long, String> entry : filters.questionnaireAnswers().entrySet()) {
                Long questionId = entry.getKey();
                String expectedAnswer = entry.getValue().toLowerCase().trim();
                String participantAnswer = participant.questionnaireAnswers().get(questionId);
                
                if (participantAnswer == null) {
                    return false;
                }
                
                // Check if answer contains the expected value (case-insensitive)
                if (!participantAnswer.toLowerCase().contains(expectedAnswer)) {
                    return false;
                }
            }
        }

        // Filter by experience level (e.g., "Senior Developer")
        if (filters.experienceLevel() != null && !filters.experienceLevel().trim().isEmpty()) {
            String level = filters.experienceLevel().toLowerCase().trim();
            String skills = participant.skills() != null ? participant.skills().toLowerCase() : "";
            String name = participant.name().toLowerCase();
            
            // Check if experience level appears in skills or name
            if (!skills.contains(level) && !name.contains(level)) {
                return false;
            }
        }

        // Filter by minimum years of experience
        if (filters.minYearsOfExperience() != null) {
            Integer participantYears = participant.yearsOfExperience();
            if (participantYears == null || participantYears < filters.minYearsOfExperience()) {
                return false;
            }
        }

        // Filter by skills
        if (filters.skills() != null && !filters.skills().trim().isEmpty()) {
            String filterSkills = filters.skills().toLowerCase().trim();
            String participantSkills = participant.skills() != null ? participant.skills().toLowerCase() : "";
            
            // Check if any of the filter skills appear in participant skills
            String[] skillArray = filterSkills.split(",");
            boolean hasAnySkill = false;
            for (String skill : skillArray) {
                if (participantSkills.contains(skill.trim())) {
                    hasAnySkill = true;
                    break;
                }
            }
            
            if (!hasAnySkill) {
                return false;
            }
        }

        return true;
    }
}

