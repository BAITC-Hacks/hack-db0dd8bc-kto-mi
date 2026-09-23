package com.qadam.service;

import lombok.Getter;

/**
 * The lesson has not been approved by a teacher yet, so it must not be shown to a child.
 */
@Getter
public class LessonNotApprovedException extends RuntimeException {

    private final long lessonId;

    public LessonNotApprovedException(long lessonId) {
        super("Lesson " + lessonId + " is not approved");
        this.lessonId = lessonId;
    }
}
