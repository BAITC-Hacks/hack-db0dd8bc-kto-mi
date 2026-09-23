package com.qadam.llm.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.dto.ClarifyingQuestion;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.model.CardField;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the hand-written JSON schemas against drifting away from the DTO records and {@link CardField}.
 */
class TaskSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void cardSchemaMatchesTaskCard() throws IOException {
        JsonNode schema = read(OpenAiLlmClient.CARD_SCHEMA_PATH);

        assertObjectMatches(schema, TaskCard.class);
        assertThat(fieldNames(schema.get("properties"))).containsExactlyElementsOf(cardFieldCodes());
    }

    @Test
    void analysisSchemaMatchesTaskAnalysis() throws IOException {
        JsonNode schema = read(OpenAiLlmClient.ANALYSIS_SCHEMA_PATH);

        assertObjectMatches(schema, TaskAnalysis.class);
        assertObjectMatches(schema.at("/properties/questions/items"), ClarifyingQuestion.class);
        assertThat(values(schema.at("/properties/missingFields/items/enum"))).containsExactlyElementsOf(cardFieldCodes());
        assertThat(values(schema.at("/properties/questions/items/properties/field/enum")))
                .containsExactlyElementsOf(cardFieldCodes());
    }

    @Test
    void codePatternListsAllCardFields() {
        assertThat(CardField.CODE_PATTERN.split("\\|")).containsExactlyElementsOf(cardFieldCodes());
    }

    private JsonNode read(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(in);
        }
    }

    private static List<String> cardFieldCodes() {
        return Arrays.stream(CardField.values()).map(CardField::getCode).toList();
    }

    private static void assertObjectMatches(JsonNode node, Class<? extends Record> type) {
        List<String> fields = Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();

        assertThat(node.get("additionalProperties").asBoolean()).as(type.getSimpleName()).isFalse();
        assertThat(fieldNames(node.get("properties"))).as(type.getSimpleName())
                .containsExactlyInAnyOrderElementsOf(fields);
        assertThat(values(node.get("required"))).as(type.getSimpleName()).containsExactlyInAnyOrderElementsOf(fields);
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static List<String> values(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }
}
