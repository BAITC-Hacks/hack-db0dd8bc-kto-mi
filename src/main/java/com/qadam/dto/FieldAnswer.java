package com.qadam.dto;

import com.qadam.model.CardField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Answer of the business to a clarifying question about one card field. A blank answer is ignored.
 */
public record FieldAnswer(
        @Schema(description = "Поле карточки", example = "data")
        @NotBlank(message = "Укажите поле карточки")
        @Pattern(regexp = CardField.CODE_PATTERN, message = "Неизвестное поле карточки")
        String field,

        @Schema(description = "Ответ бизнеса", example = "Выгрузка продаж за 2 года в Excel")
        @Size(max = 2000, message = "Ответ не должен быть длиннее {max} символов")
        String answer
) {
}
