package com.qadam.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.qadam.dto.AdaptedLesson;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores {@link AdaptedLesson} as a JSON string in a single text column.
 * Uses its own {@link ObjectMapper} so it works regardless of the Spring context.
 */
@Converter
public class AdaptedLessonConverter implements AttributeConverter<AdaptedLesson, String> {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Override
    public String convertToDatabaseColumn(AdaptedLesson attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize adapted lesson to JSON", e);
        }
    }

    @Override
    public AdaptedLesson convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, AdaptedLesson.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize adapted lesson from JSON", e);
        }
    }
}
