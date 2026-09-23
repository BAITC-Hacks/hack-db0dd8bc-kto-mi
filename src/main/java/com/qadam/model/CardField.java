package com.qadam.model;

import com.qadam.dto.TaskCard;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fields of a task card. {@link #getCode()} is the JSON property name used by the API and the LLM.
 */
@Getter
@RequiredArgsConstructor
public enum CardField {
    TITLE("title", "Название", 5),
    CONTEXT("context", "Контекст", CardField.MIN_LENGTH),
    NEED("need", "Потребность", CardField.MIN_LENGTH),
    USERS("users", "Пользователи", CardField.MIN_LENGTH),
    DATA("data", "Данные и материалы", CardField.MIN_LENGTH),
    CONSTRAINTS("constraints", "Ограничения", CardField.MIN_LENGTH),
    EXPECTED_RESULT("expectedResult", "Ожидаемый результат", CardField.MIN_LENGTH),
    SUCCESS_CRITERIA("successCriteria", "Критерии успеха", CardField.MIN_LENGTH),
    CONTACT("contact", "Контакт", 5),
    INTERACTION_FORMAT("interactionFormat", "Формат взаимодействия", CardField.MIN_LENGTH);

    /** Minimum length of a meaningfully filled field (except title and contact). */
    public static final int MIN_LENGTH = 15;

    /** Regex of all field codes, for {@code @Pattern} on request fields. */
    public static final String CODE_PATTERN =
            "title|context|need|users|data|constraints|expectedResult|successCriteria|contact|interactionFormat";

    private static final Pattern MEASURABLE = Pattern.compile("[0-9%]");

    private final String code;
    private final String label;
    private final int minLength;

    /** A field counts only if it is filled meaningfully: not blank and not shorter than {@link #minLength}. */
    public boolean isFilled(String value) {
        return value != null && value.strip().length() >= minLength;
    }

    public boolean isFilledIn(TaskCard card) {
        return isFilled(valueIn(card));
    }

    public String valueIn(TaskCard card) {
        return switch (this) {
            case TITLE -> card.title();
            case CONTEXT -> card.context();
            case NEED -> card.need();
            case USERS -> card.users();
            case DATA -> card.data();
            case CONSTRAINTS -> card.constraints();
            case EXPECTED_RESULT -> card.expectedResult();
            case SUCCESS_CRITERIA -> card.successCriteria();
            case CONTACT -> card.contact();
            case INTERACTION_FORMAT -> card.interactionFormat();
        };
    }

    /** A success criterion is measurable when it contains a number or a percentage. */
    public static boolean isMeasurable(String value) {
        return value != null && MEASURABLE.matcher(value).find();
    }

    public static Optional<CardField> fromCode(String code) {
        return Arrays.stream(values()).filter(field -> field.code.equals(code)).findFirst();
    }
}
