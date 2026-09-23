package com.qadam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A vocabulary card with an optional pictogram.
 *
 * @param pictogramUrl pictogram image URL, {@code null} if no pictogram was found
 */
public record Card(
        @NotBlank String word,
        @NotBlank String explanation,
        String pictogramUrl
) {
}
