package com.qadam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProposalRequest(
        @Schema(description = "id команды из GET /api/teams", example = "1")
        @NotNull(message = "Укажите команду")
        Long teamId,

        @Schema(description = "Идея решения")
        @NotBlank(message = "Опишите идею")
        @Size(min = 10, max = 2000, message = "Идея должна быть от {min} до {max} символов")
        String idea,

        @Schema(description = "План работ")
        @NotBlank(message = "Опишите план")
        @Size(min = 10, max = 4000, message = "План должен быть от {min} до {max} символов")
        String plan,

        @Schema(description = "Срок", example = "4 недели")
        @NotBlank(message = "Укажите срок")
        @Size(max = 100, message = "Срок не должен быть длиннее {max} символов")
        String duration,

        @Schema(description = "Ссылка на прототип (необязательно)", example = "https://example.com/prototype")
        @Size(max = 500, message = "Ссылка не должна быть длиннее {max} символов")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Ссылка должна начинаться с http:// или https://")
        String prototypeUrl
) {
}
