package com.qadam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Unified error body of every failed API call.
 *
 * @param status      HTTP status code
 * @param error       HTTP status reason phrase
 * @param message     message for the user, in Russian
 * @param fieldErrors invalid request fields; omitted when the error is not about specific fields
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorResponse> fieldErrors
) {
}
