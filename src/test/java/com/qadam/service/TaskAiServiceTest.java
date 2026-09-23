package com.qadam.service;

import com.qadam.dto.ClarifyingQuestion;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.llm.LlmClient;
import com.qadam.llm.LlmException;
import com.qadam.llm.LlmInvalidResponseException;
import com.qadam.model.Industry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskAiServiceTest {

    private static final String DRAFT = "Черновик задачи для проверки";

    private static final TaskAnalysis VALID_ANALYSIS = new TaskAnalysis(List.of("data"), List.of(
            new ClarifyingQuestion("data", "Какие данные есть?"),
            new ClarifyingQuestion("users", "Кто пользователи?"),
            new ClarifyingQuestion("contact", "Как связаться?")));

    private static final TaskAnalysis TOO_FEW_QUESTIONS = new TaskAnalysis(List.of("data"), List.of(
            new ClarifyingQuestion("data", "Какие данные есть?")));

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final LlmClient llmClient = mock(LlmClient.class);
    private final TaskAiService service = new TaskAiService(llmClient, validator);

    @Test
    void returnsValidAnalysis() {
        when(llmClient.analyze(DRAFT, Industry.IT)).thenReturn(VALID_ANALYSIS);

        assertThat(service.analyze(DRAFT, Industry.IT)).isEqualTo(VALID_ANALYSIS);
    }

    @Test
    void retriesOnceWhenAnalysisHasTooFewQuestions() {
        when(llmClient.analyze(DRAFT, Industry.IT)).thenReturn(TOO_FEW_QUESTIONS, VALID_ANALYSIS);

        assertThat(service.analyze(DRAFT, Industry.IT)).isEqualTo(VALID_ANALYSIS);
        verify(llmClient, times(2)).analyze(DRAFT, Industry.IT);
    }

    @Test
    void retriesOnceWhenQuestionIsAboutUnknownField() {
        TaskAnalysis unknownField = new TaskAnalysis(List.of(), List.of(
                new ClarifyingQuestion("budget", "Какой бюджет?"),
                new ClarifyingQuestion("users", "Кто пользователи?"),
                new ClarifyingQuestion("contact", "Как связаться?")));
        when(llmClient.analyze(DRAFT, Industry.IT)).thenReturn(unknownField, VALID_ANALYSIS);

        assertThat(service.analyze(DRAFT, Industry.IT)).isEqualTo(VALID_ANALYSIS);
    }

    @Test
    void failsAfterTwoInvalidResults() {
        when(llmClient.analyze(DRAFT, Industry.IT))
                .thenThrow(new LlmInvalidResponseException("not JSON"))
                .thenReturn(TOO_FEW_QUESTIONS);

        assertThatThrownBy(() -> service.analyze(DRAFT, Industry.IT))
                .isInstanceOf(LlmFailureException.class)
                .hasMessageContaining("2 attempts");
        verify(llmClient, times(TaskAiService.MAX_ATTEMPTS)).analyze(DRAFT, Industry.IT);
    }

    @Test
    void doesNotRetryWhenLlmIsUnavailable() {
        when(llmClient.buildCard(any(), any(), any())).thenThrow(new LlmException("HTTP 503"));

        assertThatThrownBy(() -> service.buildCard(DRAFT, Industry.IT, List.of()))
                .isInstanceOf(LlmFailureException.class)
                .hasMessageContaining("unavailable");
        verify(llmClient, times(1)).buildCard(any(), any(), any());
    }

    @Test
    void retriesWhenCardIsMissingOrTooLong() {
        TaskCard tooLong = new TaskCard("x".repeat(TaskCard.TITLE_MAX + 1), "", "", "", "", "", "", "", "", "");
        TaskCard valid = new TaskCard("Название", "", "", "", "", "", "", "", "", "");
        when(llmClient.buildCard(any(), any(), any())).thenReturn(null, tooLong, valid);

        assertThatThrownBy(() -> service.buildCard(DRAFT, Industry.IT, List.of()))
                .isInstanceOf(LlmFailureException.class);
        assertThat(service.buildCard(DRAFT, Industry.IT, List.of())).isEqualTo(valid);
    }
}
