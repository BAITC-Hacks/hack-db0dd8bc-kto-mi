package com.qadam.dto;

/**
 * A single invalid request field.
 *
 * @param field path of the field, e.g. {@code draftText} or {@code answers[0].field}
 */
public record FieldErrorResponse(String field, String message) {
}
