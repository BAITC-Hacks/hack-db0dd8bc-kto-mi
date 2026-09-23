package com.qadam.dto;

import com.qadam.model.Industry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnalyzeTaskRequest(
        @Schema(description = "Черновик задачи своими словами", example = "Мы сеть кофеен, хотим понять, почему падают продажи.")
        @NotBlank(message = "Опишите задачу")
        @Size(min = CreateTaskRequest.DRAFT_MIN, max = CreateTaskRequest.DRAFT_MAX,
                message = "Черновик должен быть от {min} до {max} символов")
        String draftText,

        @Schema(description = "Код отрасли из GET /api/industries", example = "RETAIL")
        @NotNull(message = "Выберите отрасль")
        Industry industry
) {
}
