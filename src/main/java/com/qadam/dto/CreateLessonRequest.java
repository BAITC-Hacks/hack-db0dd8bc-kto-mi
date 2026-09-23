package com.qadam.dto;

import com.qadam.model.AdaptationProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A lesson submitted by a teacher for adaptation.
 */
public record CreateLessonRequest(
        @Schema(description = "Lesson title", example = "Круговорот воды в природе")
        @NotBlank(message = "Введите название урока")
        @Size(min = 3, max = 120, message = "Название урока должно содержать от 3 до 120 символов")
        String title,

        @Schema(description = "Original lesson text")
        @NotBlank(message = "Введите текст урока")
        @Size(min = 50, max = 5000, message = "Текст урока должен содержать от 50 до 5000 символов")
        String text,

        @Schema(description = "Adaptation profile", example = "DYSLEXIA")
        @NotNull(message = "Выберите профиль адаптации")
        AdaptationProfile profile
) {
}
