// Dosya Yolu: demo/QuestionType.java
package com.halenteck.demo;

/**
 * Bir sorunun tipini belirler.
 * - MULTIPLE_CHOICE: Çoktan seçmeli (OptionEntity'ler kullanır)
 * - SHORT_ANSWER: Kısa metin cevabı (Anketler için)
 */
public enum QuestionType {
    MULTIPLE_CHOICE,
    SHORT_ANSWER,
    PARAGRAPH
}