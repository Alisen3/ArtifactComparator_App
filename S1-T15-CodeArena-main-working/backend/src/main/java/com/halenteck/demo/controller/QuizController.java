// Dosya Yolu: demo/QuizController.java
package com.halenteck.demo.controller;

import com.halenteck.demo.dto.CreateQuizRequest;
import com.halenteck.demo.dto.QuizSummaryDTO;
import com.halenteck.demo.entity.QuizEntity;
import com.halenteck.demo.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.halenteck.demo.dto.GenerateQuizRequest; // We will create this DTO
import com.halenteck.demo.dto.CreateQuestionDTO;
import com.halenteck.demo.dto.CreateOptionDTO;
import com.halenteck.demo.QuestionType;
import java.util.ArrayList;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/quizzes") // Bu controller'a /api/quizzes yoluyla erişilir
public class QuizController {

    private final QuizService quizService;

    // QuizService'i (beyin) enjekte et
    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * Endpoint 1: Yeni bir Kuis Oluştur (Issue #7)
     * Sadece Araştırmacılar (RESEARCHER) erişebilmeli.
     * Frontend'den gelen 'CreateQuizRequest' JSON'unu alır.
     * 'Principal' (giriş yapmış kullanıcı) bilgisini alır.
     */
    @PostMapping
    public ResponseEntity<QuizEntity> createQuiz(@RequestBody CreateQuizRequest request, Principal principal) {
        // İşi 'QuizService'e devret
        QuizEntity newQuiz = quizService.createQuiz(request, principal);

        // 201 Created (Oluşturuldu) durumu ve oluşturulan kuis ile yanıt dön
        return ResponseEntity.status(HttpStatus.CREATED).body(newQuiz);
    }

    /**
     * Endpoint 2: Giriş Yapan Araştırmacının Kuislerini Listele (Issue #7)
     * Sadece Araştırmacılar (RESEARCHER) erişebilmeli.
     * 'Principal' (giriş yapmış kullanıcı) bilgisini alır.
     */
    @GetMapping("/my-quizzes")
    public ResponseEntity<List<QuizSummaryDTO>> getMyQuizzes(Principal principal) {
        // İşi 'QuizService'e devret
        List<QuizSummaryDTO> quizzes = quizService.findQuizzesByCreator(principal);

        // 200 OK durumu ve DTO listesi ile yanıt dön
        return ResponseEntity.ok(quizzes);
    }

    @PostMapping("/generate")
    public ResponseEntity<List<CreateQuestionDTO>> generateQuestions(@RequestBody GenerateQuizRequest request) {
        // MOCK AI SERVICE (Simulates Issue #8)
        List<CreateQuestionDTO> questions = new ArrayList<>();
        String topic = request.topic() != null ? request.topic() : "General";

        for (int i = 1; i <= request.count(); i++) {
            List<CreateOptionDTO> opts = List.of(
                    new CreateOptionDTO("Correct Answer for " + topic, true),
                    new CreateOptionDTO("Wrong Answer 1", false),
                    new CreateOptionDTO("Wrong Answer 2", false)
            );

            questions.add(new CreateQuestionDTO(
                    "AI Generated (" + topic + "): Question " + i + "?",
                    QuestionType.MULTIPLE_CHOICE,
                    opts
            ));
        }
        return ResponseEntity.ok(questions);
    }
}