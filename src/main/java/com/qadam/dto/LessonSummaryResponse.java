package com.qadam.dto;

import com.qadam.model.AdaptationProfile;
import com.qadam.model.LessonStatus;

import java.time.Instant;

/**
 * A lesson in the teacher's lesson list, without its texts.
 */
public record LessonSummaryResponse(
        Long id,
        String title,
        AdaptationProfile profile,
        LessonStatus status,
        Instant createdAt
) {
}
