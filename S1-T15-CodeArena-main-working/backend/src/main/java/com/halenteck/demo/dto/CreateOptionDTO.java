// Dosya Yolu: demo/CreateOptionDTO.java
package com.halenteck.demo.dto;

// Java 17 'record' kullanarak DTO (Data Transfer Object) oluşturuyoruz.
// Bu, frontend'den gelen JSON'daki her bir "option" nesnesini temsil edecek.
public record CreateOptionDTO(
        String optionText,
        boolean isCorrect // Otomatik notlandırma (Issue #7) için kritik
) {
}