package com.qadam.llm.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.dto.AdaptedLesson;
import com.qadam.llm.LlmClient;
import com.qadam.model.AdaptationProfile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Offline {@link LlmClient} for demos and development. Returns prepared adaptations from
 * {@code resources/mock/} for known demo lessons and a generic demo stub for any other text.
 */
public class MockLlmClient implements LlmClient {

    static final String WATER_CYCLE = "water-cycle";
    static final String PLANT_PARTS = "plant-parts";
    static final String DEMO = "demo";

    private static final String[] LESSONS = {WATER_CYCLE, PLANT_PARTS, DEMO};

    private final Map<String, Map<AdaptationProfile, AdaptedLesson>> examples = new HashMap<>();

    public MockLlmClient(ObjectMapper objectMapper) {
        for (String lesson : LESSONS) {
            Map<AdaptationProfile, AdaptedLesson> byProfile = new EnumMap<>(AdaptationProfile.class);
            for (AdaptationProfile profile : AdaptationProfile.values()) {
                byProfile.put(profile, read(objectMapper, resourcePath(lesson, profile)));
            }
            examples.put(lesson, byProfile);
        }
    }

    @Override
    public AdaptedLesson adapt(String title, String text, AdaptationProfile profile) {
        return examples.get(lessonKey(title)).get(profile);
    }

    /**
     * Picks a prepared example by lesson title; unknown titles get the demo stub.
     */
    static String lessonKey(String title) {
        String normalized = title == null ? "" : title.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^\\p{L}\\p{N}]+", " ");
        if (normalized.contains("круговорот")) {
            return WATER_CYCLE;
        }
        if (normalized.contains("част") && normalized.contains("растен")) {
            return PLANT_PARTS;
        }
        return DEMO;
    }

    static String resourcePath(String lesson, AdaptationProfile profile) {
        return "mock/" + lesson + "-" + profile.name().toLowerCase(Locale.ROOT) + ".json";
    }

    private static AdaptedLesson read(ObjectMapper objectMapper, String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, AdaptedLesson.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read mock adaptation " + path, e);
        }
    }
}
