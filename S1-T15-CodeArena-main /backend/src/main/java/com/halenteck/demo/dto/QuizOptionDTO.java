// Dosya Yolu: demo/QuizOptionDTO.java
package com.halenteck.demo.dto;

// Katılımcıya gönderilecek GÜVENLİ şık DTO'su
// 'isCorrect' alanını KESİNLİKLE içermez.
public record QuizOptionDTO(
        Long id,
        String optionText
) {
}