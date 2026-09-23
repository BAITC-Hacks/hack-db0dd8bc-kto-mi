package com.qadam.dto;

import com.qadam.model.AdaptationProfile;
import com.qadam.model.LessonStatus;

import java.time.Instant;

/**
 * A lesson as seen by a teacher: original text, adapted content and review status.
 *
 * @param profileName human-readable profile name
 */
public record LessonResponse(
        Long id,
        String title,
        String originalText,
        AdaptationProfile profile,
        String profileName,
        LessonStatus status,
        Instant createdAt,
        Instant updatedAt,
        AdaptedLesson content
) {
}
