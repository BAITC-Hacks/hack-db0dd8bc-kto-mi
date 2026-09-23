package com.qadam.llm.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.Card;
import com.qadam.dto.Question;
import com.qadam.dto.Section;
import com.qadam.dto.Sentence;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the hand-written JSON schema against drifting away from the DTO records.
 */
class AdaptedLessonSchemaTest {

    /** Filled in by the pictogram lookup, never requested from the model. */
    private static final Set<String> NOT_GENERATED = Set.of("pictogramUrl");

    private static JsonNode schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream in = new ClassPathResource(OpenAiLlmClient.SCHEMA_PATH).getInputStream()) {
            schema = new ObjectMapper().readTree(in);
        }
    }

    @Test
    void schemaMatchesAdaptedLessonRecords() {
        assertObjectMatches(schema, AdaptedLesson.class);
        JsonNode properties = schema.get("properties");
        assertObjectMatches(properties.at("/sentences/items"), Sentence.class);
        assertObjectMatches(properties.at("/cards/items"), Card.class);
        assertObjectMatches(properties.at("/quiz/items"), Question.class);
    }

    @Test
    void sectionEnumMatchesSectionValuesAndAllowsNull() {
        JsonNode section = schema.at("/properties/sentences/items/properties/section");
        List<String> values = new ArrayList<>();
        section.get("enum").forEach(value -> values.add(value.isNull() ? null : value.asText()));

        List<String> expected = new ArrayList<>(Arrays.stream(Section.values()).map(Enum::name).toList());
        expected.add(null);
        assertThat(values).containsExactlyElementsOf(expected);
    }

    private static void assertObjectMatches(JsonNode node, Class<? extends Record> type) {
        List<String> fields = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .filter(name -> !NOT_GENERATED.contains(name))
                .toList();
        List<String> properties = new ArrayList<>();
        node.get("properties").fieldNames().forEachRemaining(properties::add);
        List<String> required = new ArrayList<>();
        node.get("required").forEach(name -> required.add(name.asText()));

        assertThat(node.get("additionalProperties").asBoolean()).as(type.getSimpleName()).isFalse();
        assertThat(properties).as(type.getSimpleName()).containsExactlyInAnyOrderElementsOf(fields);
        assertThat(required).as(type.getSimpleName()).containsExactlyInAnyOrderElementsOf(fields);
    }
}
