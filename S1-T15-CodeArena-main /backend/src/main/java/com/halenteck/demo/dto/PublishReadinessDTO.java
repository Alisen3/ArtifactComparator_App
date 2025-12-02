package com.halenteck.demo.dto;

import java.util.List;

public record PublishReadinessDTO(
        boolean ready,
        List<String> errors,
        int draftVersionNumber,
        Integer latestPublishedVersionNumber
) {
}

