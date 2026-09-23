package com.qadam.llm;

import com.qadam.model.AdaptationProfile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Builds the system and user messages for lesson adaptation from {@code prompts/adapt-lesson.md}.
 */
public class AdaptationPrompt {

    static final String TEMPLATE_PATH = "prompts/adapt-lesson.md";

    private final String template;

    public AdaptationPrompt() {
        this.template = readTemplate();
    }

    public String systemPrompt(AdaptationProfile profile) {
        String rules = profile.getAdaptationRules().stream()
                .map(rule -> "- " + rule)
                .collect(Collectors.joining("\n"));
        return template
                .replace("{{profileName}}", profile.getDisplayName())
                .replace("{{profileRules}}", rules);
    }

    public String userPrompt(String title, String text) {
        return "Lesson title: " + title + "\n\nLesson text:\n" + text;
    }

    private static String readTemplate() {
        try {
            return new ClassPathResource(TEMPLATE_PATH).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read prompt template " + TEMPLATE_PATH, e);
        }
    }
}
