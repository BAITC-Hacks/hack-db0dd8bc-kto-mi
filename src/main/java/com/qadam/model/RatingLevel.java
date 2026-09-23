package com.qadam.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Readiness level of a task card derived from its rating score (0–100).
 */
@Getter
@RequiredArgsConstructor
public enum RatingLevel {
    DRAFT(0, "Черновик"),
    WORKING(40, "Рабочая задача"),
    READY(70, "Готова к работе"),
    PRIORITY(90, "Приоритетная");

    /** Lowest score of the level; the level ends right before the next level's minimum. */
    private final int minScore;
    private final String displayName;

    public static RatingLevel of(int score) {
        RatingLevel result = DRAFT;
        for (RatingLevel level : values()) {
            if (score >= level.minScore) {
                result = level;
            }
        }
        return result;
    }
}
