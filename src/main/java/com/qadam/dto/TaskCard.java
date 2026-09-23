package com.qadam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Card of a business task. Missing information is an empty string, never {@code null}:
 * the compact constructor trims every field and turns {@code null} into {@code ""}.
 */
@Schema(description = "Карточка задачи. Незаполненное поле — пустая строка")
public record TaskCard(
        @Schema(description = "Название задачи") @Size(max = TaskCard.TITLE_MAX) String title,
        @Schema(description = "Контекст: кто заказчик и чем занимается") @Size(max = TaskCard.FIELD_MAX) String context,
        @Schema(description = "Потребность: какую проблему нужно решить") @Size(max = TaskCard.FIELD_MAX) String need,
        @Schema(description = "Пользователи решения") @Size(max = TaskCard.FIELD_MAX) String users,
        @Schema(description = "Данные и материалы, которые получит команда") @Size(max = TaskCard.FIELD_MAX) String data,
        @Schema(description = "Ограничения: сроки, бюджет, технологии, правила") @Size(max = TaskCard.FIELD_MAX) String constraints,
        @Schema(description = "Ожидаемый результат") @Size(max = TaskCard.FIELD_MAX) String expectedResult,
        @Schema(description = "Критерии успеха, лучше с цифрами") @Size(max = TaskCard.FIELD_MAX) String successCriteria,
        @Schema(description = "Контакт представителя бизнеса") @Size(max = TaskCard.CONTACT_MAX) String contact,
        @Schema(description = "Формат взаимодействия с командой") @Size(max = TaskCard.FIELD_MAX) String interactionFormat
) {

    public static final int TITLE_MAX = 200;
    public static final int FIELD_MAX = 3000;
    public static final int CONTACT_MAX = 300;

    public TaskCard {
        title = clean(title);
        context = clean(context);
        need = clean(need);
        users = clean(users);
        data = clean(data);
        constraints = clean(constraints);
        expectedResult = clean(expectedResult);
        successCriteria = clean(successCriteria);
        contact = clean(contact);
        interactionFormat = clean(interactionFormat);
    }

    public static TaskCard empty() {
        return new TaskCard(null, null, null, null, null, null, null, null, null, null);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
