package com.qadam.dto;

import com.qadam.model.Industry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param answers answers to the clarifying questions from {@code POST /api/tasks/analyze}; may be empty
 */
public record CreateTaskRequest(
        @Schema(description = "Черновик задачи своими словами")
        @NotBlank(message = "Опишите задачу")
        @Size(min = DRAFT_MIN, max = DRAFT_MAX, message = "Черновик должен быть от {min} до {max} символов")
        String draftText,

        @Schema(description = "Код отрасли из GET /api/industries", example = "RETAIL")
        @NotNull(message = "Выберите отрасль")
        Industry industry,

        @Size(max = 20, message = "Не больше {max} ответов")
        List<@Valid @NotNull FieldAnswer> answers
) {

    public static final int DRAFT_MIN = 20;
    public static final int DRAFT_MAX = 5000;

    public CreateTaskRequest {
        answers = answers == null ? List.of() : answers;
    }
}
