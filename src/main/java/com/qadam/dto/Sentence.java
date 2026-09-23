package com.qadam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * A single adapted sentence.
 *
 * @param text     sentence text
 * @param keywords key words to highlight; may be empty
 * @param section  First / Then / Finally section for the AUTISM profile, {@code null} otherwise
 */
public record Sentence(
        @NotBlank String text,
        @NotNull List<@NotBlank String> keywords,
        Section section
) {
}
