package com.qadam.service;

import com.qadam.dto.AdaptedLesson;
import com.qadam.llm.LlmClient;
import com.qadam.llm.LlmException;
import com.qadam.llm.LlmInvalidResponseException;
import com.qadam.model.AdaptationProfile;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Adapts a lesson through the {@link LlmClient} and makes sure the result is a valid {@link AdaptedLesson}.
 * An unparseable or invalid result is retried once; an unavailable LLM is not retried.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonAdaptationService {

    static final int MAX_ATTEMPTS = 2;

    private final LlmClient llmClient;
    private final Validator validator;

    public AdaptedLesson adapt(String title, String text, AdaptationProfile profile) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return validated(requestAdaptation(title, text, profile));
            } catch (LlmInvalidResponseException | ConstraintViolationException e) {
                lastError = e;
                log.warn("Adaptation attempt {}/{} for profile {} failed: {}",
                        attempt, MAX_ATTEMPTS, profile, e.getMessage());
            }
        }
        throw new LlmAdaptationException(
                "LLM returned an invalid adaptation after " + MAX_ATTEMPTS + " attempts: " + lastError.getMessage(),
                lastError);
    }

    private AdaptedLesson requestAdaptation(String title, String text, AdaptationProfile profile) {
        try {
            return llmClient.adapt(title, text, profile);
        } catch (LlmInvalidResponseException e) {
            throw e;
        } catch (LlmException e) {
            throw new LlmAdaptationException("LLM is unavailable: " + e.getMessage(), e);
        }
    }

    private AdaptedLesson validated(AdaptedLesson lesson) {
        if (lesson == null) {
            throw new LlmInvalidResponseException("LLM returned no adapted lesson");
        }
        Set<ConstraintViolation<AdaptedLesson>> violations = validator.validate(lesson);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return lesson;
    }
}
