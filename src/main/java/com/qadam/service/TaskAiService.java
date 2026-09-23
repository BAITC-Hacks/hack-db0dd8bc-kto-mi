package com.qadam.service;

import com.qadam.dto.FieldAnswer;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.llm.LlmClient;
import com.qadam.llm.LlmException;
import com.qadam.llm.LlmInvalidResponseException;
import com.qadam.model.Industry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Calls the {@link LlmClient} and makes sure the result is valid (Bean Validation).
 * An unparseable or invalid result is retried once; an unavailable LLM is not retried.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAiService {

    static final int MAX_ATTEMPTS = 2;

    private final LlmClient llmClient;
    private final Validator validator;

    public TaskAnalysis analyze(String draftText, Industry industry) {
        return withRetry("analyze", () -> llmClient.analyze(draftText, industry));
    }

    public TaskCard buildCard(String draftText, Industry industry, List<FieldAnswer> answers) {
        return withRetry("buildCard", () -> llmClient.buildCard(draftText, industry, answers));
    }

    private <T> T withRetry(String operation, Supplier<T> call) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return validated(request(call));
            } catch (LlmInvalidResponseException | ConstraintViolationException e) {
                lastError = e;
                log.warn("LLM {} attempt {}/{} failed: {}", operation, attempt, MAX_ATTEMPTS, e.getMessage());
            }
        }
        throw new LlmFailureException(
                "LLM returned an invalid " + operation + " result after " + MAX_ATTEMPTS + " attempts: "
                        + lastError.getMessage(),
                lastError);
    }

    private static <T> T request(Supplier<T> call) {
        try {
            return call.get();
        } catch (LlmInvalidResponseException e) {
            throw e;
        } catch (LlmException e) {
            throw new LlmFailureException("LLM is unavailable: " + e.getMessage(), e);
        }
    }

    private <T> T validated(T result) {
        if (result == null) {
            throw new LlmInvalidResponseException("LLM returned no result");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(result);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return result;
    }
}
