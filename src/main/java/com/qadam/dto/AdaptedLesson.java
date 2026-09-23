package com.qadam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Adapted version of a lesson: simplified sentences, vocabulary cards and a quiz.
 */
public record AdaptedLesson(
        @NotEmpty List<@Valid Sentence> sentences,
        @NotEmpty List<@Valid Card> cards,
        @NotEmpty List<@Valid Question> quiz
) {
}
