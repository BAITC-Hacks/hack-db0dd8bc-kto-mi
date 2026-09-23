package com.qadam.model;

import com.qadam.dto.AdaptedLesson;
import org.junit.jupiter.api.Test;

import static com.qadam.dto.AdaptedLessonFixtures.validLesson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdaptedLessonConverterTest {

    private final AdaptedLessonConverter converter = new AdaptedLessonConverter();

    @Test
    void roundTripPreservesContent() {
        AdaptedLesson lesson = validLesson();

        String json = converter.convertToDatabaseColumn(lesson);
        AdaptedLesson restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isEqualTo(lesson);
    }

    @Test
    void serializesExpectedJsonShape() {
        String json = converter.convertToDatabaseColumn(validLesson());

        assertThat(json)
                .contains("\"sentences\"", "\"cards\"", "\"quiz\"")
                .contains("\"section\":\"FIRST\"")
                .contains("\"correctIndex\":0")
                .doesNotContain("correctIndexValid");
    }

    @Test
    void ignoresUnknownProperties() {
        String json = """
                {"sentences":[{"text":"Hi.","keywords":[],"section":null,"extra":1}],
                 "cards":[{"word":"hi","explanation":"A greeting.","pictogramUrl":null}],
                 "quiz":[{"question":"Q?","options":["A"],"correctIndex":0}],
                 "unknown":"value"}
                """;

        AdaptedLesson lesson = converter.convertToEntityAttribute(json);

        assertThat(lesson.sentences()).hasSize(1);
        assertThat(lesson.sentences().getFirst().section()).isNull();
        assertThat(lesson.cards().getFirst().pictogramUrl()).isNull();
    }

    @Test
    void nullMapsToNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void invalidJsonThrows() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("{not json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
