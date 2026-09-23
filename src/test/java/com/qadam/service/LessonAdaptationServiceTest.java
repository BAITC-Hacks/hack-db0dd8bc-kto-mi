package com.qadam.service;

import com.qadam.dto.AdaptedLesson;
import com.qadam.llm.LlmClient;
import com.qadam.llm.LlmException;
import com.qadam.llm.LlmInvalidResponseException;
import com.qadam.model.AdaptationProfile;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import static com.qadam.dto.AdaptedLessonFixtures.validLesson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonAdaptationServiceTest {

    private static ValidatorFactory factory;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void returnsValidLessonOnFirstAttempt() {
        ScriptedLlmClient llm = new ScriptedLlmClient(List.of(() -> validLesson()));

        assertThat(adapt(llm)).isEqualTo(validLesson());
        assertThat(llm.calls).isEqualTo(1);
    }

    @Test
    void retriesOnceAfterUnparseableResponse() {
        ScriptedLlmClient llm = new ScriptedLlmClient(List.of(
                () -> {
                    throw new LlmInvalidResponseException("not JSON");
                },
                () -> validLesson()));

        assertThat(adapt(llm)).isEqualTo(validLesson());
        assertThat(llm.calls).isEqualTo(2);
    }

    @Test
    void retriesOnceAfterInvalidLesson() {
        ScriptedLlmClient llm = new ScriptedLlmClient(List.of(() -> invalidLesson(), () -> validLesson()));

        assertThat(adapt(llm)).isEqualTo(validLesson());
        assertThat(llm.calls).isEqualTo(2);
    }

    @Test
    void failsAfterTwoUnparseableResponses() {
        ScriptedLlmClient llm = new ScriptedLlmClient(List.of(
                () -> {
                    throw new LlmInvalidResponseException("first");
                },
                () -> {
                    throw new LlmInvalidResponseException("second");
                }));

        assertThatThrownBy(() -> adapt(llm))
                .isInstanceOf(LlmAdaptationException.class)
                .hasMessageContaining("after 2 attempts")
                .hasMessageContaining("second")
                .hasCauseInstanceOf(LlmInvalidResponseException.class);
        assertThat(llm.calls).isEqualTo(2);
    }

    @Test
    void failsAfterTwoInvalidLessons() {
        ScriptedLlmClient llm = new ScriptedLlmClient(List.of(() -> invalidLesson(), () -> null));

        assertThatThrownBy(() -> adapt(llm))
                .isInstanceOf(LlmAdaptationException.class)
                .hasCauseInstanceOf(LlmInvalidResponseException.class);
        assertThat(llm.calls).isEqualTo(2);
    }

    @Test
    void reportsViolationsOfInvalidLesson() {
        ScriptedLlmClient llm = new ScriptedLlmClient(List.of(() -> invalidLesson(), () -> invalidLesson()));

        assertThatThrownBy(() -> adapt(llm))
                .isInstanceOf(LlmAdaptationException.class)
                .hasMessageContaining("cards")
                .hasCauseInstanceOf(ConstraintViolationException.class);
        assertThat(llm.calls).isEqualTo(2);
    }

    @Test
    void doesNotRetryWhenLlmIsUnavailable() {
        ScriptedLlmClient llm = new ScriptedLlmClient(List.of(
                () -> {
                    throw new LlmException("OpenAI API returned HTTP 503");
                },
                () -> validLesson()));

        assertThatThrownBy(() -> adapt(llm))
                .isInstanceOf(LlmAdaptationException.class)
                .hasMessageContaining("503")
                .hasCauseInstanceOf(LlmException.class);
        assertThat(llm.calls).isEqualTo(1);
    }

    private static AdaptedLesson adapt(LlmClient llm) {
        return new LessonAdaptationService(llm, factory.getValidator())
                .adapt("Урок", "Текст урока.", AdaptationProfile.DYSLEXIA);
    }

    private static AdaptedLesson invalidLesson() {
        AdaptedLesson lesson = validLesson();
        return new AdaptedLesson(lesson.sentences(), List.of(), lesson.quiz());
    }

    /**
     * Returns scripted results in order and counts calls.
     */
    private static final class ScriptedLlmClient implements LlmClient {

        private final Deque<Supplier<AdaptedLesson>> responses;
        private int calls;

        ScriptedLlmClient(List<Supplier<AdaptedLesson>> responses) {
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public AdaptedLesson adapt(String title, String text, AdaptationProfile profile) {
            calls++;
            return responses.removeFirst().get();
        }
    }
}
