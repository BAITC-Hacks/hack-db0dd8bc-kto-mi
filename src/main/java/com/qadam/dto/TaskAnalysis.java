package com.qadam.dto;

import com.qadam.model.CardField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Result of analyzing a draft: which card fields are missing and what to ask the business.
 * Returned by the {@link com.qadam.llm.LlmClient} and by {@code POST /api/tasks/analyze}.
 */
public record TaskAnalysis(
        @NotNull List<@NotNull @Pattern(regexp = CardField.CODE_PATTERN) String> missingFields,
        @NotNull @Size(min = MIN_QUESTIONS) List<@Valid @NotNull ClarifyingQuestion> questions
) {

    public static final int MIN_QUESTIONS = 3;
}
