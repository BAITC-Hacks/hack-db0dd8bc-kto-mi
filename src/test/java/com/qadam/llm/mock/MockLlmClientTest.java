package com.qadam.llm.mock;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.Question;
import com.qadam.dto.Section;
import com.qadam.dto.Sentence;
import com.qadam.model.AdaptationProfile;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MockLlmClientTest {

    private static final String WATER_CYCLE_TITLE = "Круговорот воды в природе";
    private static final String PLANT_PARTS_TITLE = "Части растения";
    private static final String UNKNOWN_TITLE = "Таблица умножения";

    private static ValidatorFactory factory;
    private static Validator validator;
    private static MockLlmClient client;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        client = new MockLlmClient(JsonMapper.builder().findAndAddModules().build());
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    static Stream<Arguments> allExamples() {
        return Stream.of(WATER_CYCLE_TITLE, PLANT_PARTS_TITLE, UNKNOWN_TITLE)
                .flatMap(title -> Arrays.stream(AdaptationProfile.values())
                        .map(profile -> Arguments.of(title, profile)));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("allExamples")
    void examplesAreValidAndFollowCommonRules(String title, AdaptationProfile profile) {
        AdaptedLesson lesson = client.adapt(title, "text", profile);

        assertThat(validator.validate(lesson)).isEmpty();
        assertThat(lesson.cards()).hasSizeBetween(5, 8);
        assertThat(lesson.cards()).allSatisfy(card -> {
            assertThat(card.word()).doesNotContain(" ");
            assertThat(card.pictogramUrl()).isNull();
        });
        assertThat(lesson.quiz()).hasSizeBetween(3, 5);
        assertThat(lesson.quiz()).extracting(Question::options).allSatisfy(options -> assertThat(options).hasSize(3));
        assertThat(lesson.sentences()).allSatisfy(sentence ->
                assertThat(sentence.keywords()).allSatisfy(keyword ->
                        assertThat(sentence.text()).contains(keyword)));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("allExamples")
    void examplesFollowProfileRules(String title, AdaptationProfile profile) {
        AdaptedLesson lesson = client.adapt(title, "text", profile);

        switch (profile) {
            case DYSLEXIA -> assertThat(lesson.sentences()).allSatisfy(sentence -> {
                assertThat(sentence.section()).isNull();
                assertThat(wordCount(sentence.text())).isLessThanOrEqualTo(12);
            });
            case AUTISM -> {
                assertThat(lesson.sentences()).extracting(Sentence::section)
                        .doesNotContainNull()
                        .contains(Section.FIRST, Section.THEN, Section.FINALLY)
                        .isSortedAccordingTo(Enum::compareTo);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {WATER_CYCLE_TITLE, "круговорот воды", "  КРУГОВОРОТ ВОДЫ В ПРИРОДЕ! "})
    void waterCycleTitlesSelectWaterCycleExample(String title) {
        assertThat(MockLlmClient.lessonKey(title)).isEqualTo(MockLlmClient.WATER_CYCLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {PLANT_PARTS_TITLE, "части растений", "Урок: «Части растения»"})
    void plantPartsTitlesSelectPlantPartsExample(String title) {
        assertThat(MockLlmClient.lessonKey(title)).isEqualTo(MockLlmClient.PLANT_PARTS);
    }

    @Test
    void unknownLessonReturnsDemoStub() {
        assertThat(MockLlmClient.lessonKey(UNKNOWN_TITLE)).isEqualTo(MockLlmClient.DEMO);
        assertThat(MockLlmClient.lessonKey(null)).isEqualTo(MockLlmClient.DEMO);

        for (AdaptationProfile profile : AdaptationProfile.values()) {
            AdaptedLesson lesson = client.adapt(UNKNOWN_TITLE, "Любой текст", profile);
            assertThat(lesson.sentences().getFirst().text()).contains("демо-режиме");
        }
    }

    @Test
    void knownLessonsAreNotDemoStubs() {
        for (AdaptationProfile profile : AdaptationProfile.values()) {
            assertThat(client.adapt(WATER_CYCLE_TITLE, "text", profile).sentences())
                    .extracting(Sentence::text)
                    .noneMatch(text -> text.contains("демо"))
                    .anyMatch(text -> text.contains("пар"));
            assertThat(client.adapt(PLANT_PARTS_TITLE, "text", profile).sentences())
                    .extracting(Sentence::text)
                    .anyMatch(text -> text.contains("стебель"));
        }
    }

    private static long wordCount(String text) {
        return Arrays.stream(text.split("\\s+"))
                .filter(token -> token.codePoints().anyMatch(Character::isLetterOrDigit))
                .count();
    }
}
