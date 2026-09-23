package com.qadam.dto;

/**
 * A single invalid request field.
 *
 * @param field path of the field, e.g. {@code title} or {@code quiz[0].options}
 */
public record FieldErrorResponse(String field, String message) {
}
