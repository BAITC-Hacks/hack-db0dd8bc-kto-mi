package com.qadam.dto;

import com.qadam.model.DisplaySettings;

/**
 * An approved lesson as seen by a child: only the adapted content and how to display it.
 */
public record StudentLessonResponse(
        String title,
        DisplaySettings displaySettings,
        AdaptedLesson content
) {
}
