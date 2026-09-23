package com.qadam.dto;

import com.qadam.model.CardField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * A question the AI asks the business about one card field.
 */
public record ClarifyingQuestion(
        @NotBlank @Pattern(regexp = CardField.CODE_PATTERN) String field,
        @NotBlank String question
) {
}
