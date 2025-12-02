package com.halenteck.demo.dto;

import com.halenteck.demo.UserRole;

// React'e (frontend) döneceğimiz cevap bu olacak
// 'record' kullanmak, constructor, getter, vb. metodları otomatik oluşturur.
public record AuthResponse(
        String accessToken,
        Long id,
        String name,
        String email,
        UserRole role
) {
    // "Bearer" token tipini de ekleyebiliriz (iyi bir pratiktir)
    public String getTokenType() {
        return "Bearer";
    }
}