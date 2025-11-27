package com.halenteck.demo.dto;

public record CreateStudyRequest(
        String title,
        String description,
        Boolean blinded // Optional, defaults to false if null
) {
}
