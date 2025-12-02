package com.halenteck.demo.dto;

import java.time.LocalDateTime;

public record UpdateStudyRequest(
        String title,
        String description,
        Boolean blinded,
        LocalDateTime accessWindowStart,
        LocalDateTime accessWindowEnd
) {
}


