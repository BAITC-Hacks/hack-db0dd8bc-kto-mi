package com.qadam.dto;

import java.util.List;

public final class AdaptedLessonFixtures {

    private AdaptedLessonFixtures() {
    }

    public static AdaptedLesson validLesson() {
        return new AdaptedLesson(
                List.of(
                        new Sentence("First, the seed goes into the soil.", List.of("seed", "soil"), Section.FIRST),
                        new Sentence("Then the seed gets water.", List.of("water"), Section.THEN),
                        new Sentence("Plants need light.", List.of(), null)
                ),
                List.of(
                        new Card("seed", "A small part of a plant that grows into a new plant.",
                                "https://example.org/pictograms/seed.png"),
                        new Card("soil", "The ground where plants grow.", null)
                ),
                List.of(validQuestion())
        );
    }

    public static Question validQuestion() {
        return new Question("What does a seed need to grow?", List.of("Water", "Sand", "Noise"), 0);
    }
}
