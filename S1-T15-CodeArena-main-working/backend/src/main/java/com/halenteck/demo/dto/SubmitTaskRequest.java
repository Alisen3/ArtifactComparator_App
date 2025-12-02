// File: backend/src/main/java/com/halenteck/demo/dto/SubmitTaskRequest.java
package com.halenteck.demo.dto;

public record SubmitTaskRequest(
        String annotations,
        
        Double clarityA,
        Double relevanceA,
        Double accuracyA,
        String commentA,
        String highlightDataA, // <--- NEW

        Double clarityB,
        Double relevanceB,
        Double accuracyB,
        String commentB,
        String highlightDataB  // <--- NEW
) {
}
