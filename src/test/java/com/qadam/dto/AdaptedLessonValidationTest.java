package com.qadam.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static com.qadam.dto.AdaptedLessonFixtures.validLesson;
import static com.qadam.dto.AdaptedLessonFixtures.validQuestion;
import static org.assertj.core.api.Assertions.assertThat;

class AdaptedLessonValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validLessonHasNoViolations() {
        assertThat(validator.validate(validLesson())).isEmpty();
    }

    @Test
    void emptySentencesAreRejected() {
        AdaptedLesson lesson = validLesson();
        assertViolationOn(new AdaptedLesson(List.of(), lesson.cards(), lesson.quiz()), "sentences");
    }

    @Test
    void emptyCardsAreRejected() {
        AdaptedLesson lesson = validLesson();
        assertViolationOn(new AdaptedLesson(lesson.sentences(), List.of(), lesson.quiz()), "cards");
    }

    @Test
    void emptyQuizIsRejected() {
        AdaptedLesson lesson = validLesson();
        assertViolationOn(new AdaptedLesson(lesson.sentences(), lesson.cards(), List.of()), "quiz");
    }

    @Test
    void blankSentenceTextIsRejected() {
        AdaptedLesson lesson = validLesson();
        AdaptedLesson invalid = new AdaptedLesson(
                List.of(new Sentence(" ", List.of(), null)), lesson.cards(), lesson.quiz());
        assertViolationOn(invalid, "sentences[0].text");
    }

    @Test
    void emptyOptionsAreRejected() {
        Question question = new Question("Question?", List.of(), 0);
        assertViolationOn(withQuestion(question), "quiz[0].options");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 10})
    void correctIndexOutOfRangeIsRejected(int correctIndex) {
        Question valid = validQuestion();
        Question question = new Question(valid.question(), valid.options(), correctIndex);
        assertViolationOn(withQuestion(question), "quiz[0].correctIndexValid");
    }

    @Test
    void lastOptionIsAValidCorrectIndex() {
        Question valid = validQuestion();
        Question question = new Question(valid.question(), valid.options(), valid.options().size() - 1);
        assertThat(validator.validate(withQuestion(question))).isEmpty();
    }

    private static AdaptedLesson withQuestion(Question question) {
        AdaptedLesson lesson = validLesson();
        return new AdaptedLesson(lesson.sentences(), lesson.cards(), List.of(question));
    }

    private static void assertViolationOn(AdaptedLesson lesson, String path) {
        Set<ConstraintViolation<AdaptedLesson>> violations = validator.validate(lesson);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(path);
    }
}
