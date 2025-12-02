// Dosya Yolu: demo/AssignQuizRequest.java
package com.halenteck.demo.dto;

// Bir çalışmaya kuis atamak için kullanılacak DTO.
// Sadece atanacak kuis'in ID'sini taşır.
public record AssignQuizRequest(
        Long quizId
) {
}