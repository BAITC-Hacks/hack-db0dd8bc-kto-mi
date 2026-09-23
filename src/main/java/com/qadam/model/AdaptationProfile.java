package com.qadam.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Adaptation profile of a lesson. Describes how the material is adapted, never who it is for:
 * no data about a specific child or diagnosis is stored.
 */
@Getter
@RequiredArgsConstructor
public enum AdaptationProfile {

    DYSLEXIA(
            "Дислексия",
            List.of(
                    "Use short sentences of at most 10-12 words.",
                    "Use simple, high-frequency everyday words; avoid rare and abstract terms.",
                    "Express exactly one idea per sentence.",
                    "Mark the key words of every sentence so they can be highlighted.",
                    "Avoid complex grammatical constructions, subordinate clauses and passive voice.",
                    "Avoid long enumerations; split them into separate short sentences."
            ),
            new DisplaySettings(20, 1.8, true, "#FDF6E3", true, false)
    ),

    AUTISM(
            "Аутизм (РАС)",
            List.of(
                    "Use literal language only: no metaphors, idioms, irony or sarcasm.",
                    "Follow a clear, predictable structure: First, Then, Finally.",
                    "Assign every sentence to one section: FIRST, THEN or FINALLY.",
                    "Explain ideas with concrete, real-life examples.",
                    "Avoid any ambiguity: one word has one meaning, no vague references.",
                    "Keep sentences short and direct."
            ),
            new DisplaySettings(18, 1.6, false, "#F4F6F8", true, true)
    );

    /** Human-readable profile name shown in the interface. */
    private final String displayName;

    /** Text adaptation rules passed to the LLM prompt. */
    private final List<String> adaptationRules;

    /** Settings the frontend applies when rendering the adapted lesson. */
    private final DisplaySettings displaySettings;
}
