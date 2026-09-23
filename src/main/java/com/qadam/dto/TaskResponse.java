package com.qadam.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.qadam.model.Industry;
import com.qadam.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * A task with its card fields (at the top level) and the current rating.
 *
 * @param needsClarification {@code true} for tasks of the {@code DRAFT} rating level
 * @param clarificationNote  «требует уточнения» for such tasks, otherwise {@code null}
 */
public record TaskResponse(
        Long id,
        Industry industry,
        String industryName,
        TaskStatus status,
        String draftText,
        @JsonUnwrapped @Schema(implementation = TaskCard.class) TaskCard card,
        Rating rating,
        boolean needsClarification,
        String clarificationNote,
        Instant createdAt,
        Instant updatedAt
) {

    public static final String CLARIFICATION_NOTE = "требует уточнения";
}
