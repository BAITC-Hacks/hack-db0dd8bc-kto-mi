package com.qadam.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * A multiple-choice quiz question.
 *
 * @param correctIndex zero-based index of the correct option
 */
public record Question(
        @NotBlank String question,
        @NotEmpty List<@NotBlank String> options,
        int correctIndex
) {

    @JsonIgnore
    @AssertTrue(message = "correctIndex должен указывать на существующий вариант ответа")
    public boolean isCorrectIndexValid() {
        return options != null && correctIndex >= 0 && correctIndex < options.size();
    }
}
