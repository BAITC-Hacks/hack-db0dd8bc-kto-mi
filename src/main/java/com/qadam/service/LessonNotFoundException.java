package com.qadam.service;

import lombok.Getter;

/**
 * No lesson exists with the requested id.
 */
@Getter
public class LessonNotFoundException extends RuntimeException {

    private final long lessonId;

    public LessonNotFoundException(long lessonId) {
        super("Lesson " + lessonId + " not found");
        this.lessonId = lessonId;
    }
}
