package com.qadam.llm.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.Section;
import com.qadam.llm.AdaptationPrompt;
import com.qadam.llm.LlmException;
import com.qadam.llm.LlmInvalidResponseException;
import com.qadam.model.AdaptationProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiLlmClientTest {

    private static final String BASE_URL = "https://api.openai.test/v1";
    private static final String MODEL = "test-model";

    private static final String LESSON_JSON = """
            {
              "sentences": [
                {"text": "Сначала Солнце нагревает воду.", "keywords": ["Солнце"], "section": "FIRST"}
              ],
              "cards": [
                {"word": "вода", "explanation": "Вода есть в реке."}
              ],
              "quiz": [
                {"question": "Что нагревает воду?", "options": ["Солнце", "Луна", "Ветер"], "correctIndex": 0}
              ]
            }
            """;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    private MockRestServiceServer server;
    private OpenAiLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiLlmClient(builder.build(), MODEL, new AdaptationPrompt(), objectMapper);
    }

    @Test
    void sendsStructuredOutputRequestAndParsesResponse() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content", containsString("Аутизм (РАС)")))
                .andExpect(jsonPath("$.messages[0].content",
                        containsString(AdaptationProfile.AUTISM.getAdaptationRules().getFirst())))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content", containsString("Круговорот воды")))
                .andExpect(jsonPath("$.messages[1].content", containsString("Солнце греет воду.")))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("adapted_lesson"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.required[0]").value("sentences"))
                .andRespond(withSuccess(completion(LESSON_JSON, null, "stop"), MediaType.APPLICATION_JSON));

        AdaptedLesson lesson = client.adapt("Круговорот воды", "Солнце греет воду.", AdaptationProfile.AUTISM);

        server.verify();
        assertThat(lesson.sentences()).singleElement().satisfies(sentence -> {
            assertThat(sentence.text()).isEqualTo("Сначала Солнце нагревает воду.");
            assertThat(sentence.section()).isEqualTo(Section.FIRST);
        });
        assertThat(lesson.cards()).singleElement().satisfies(card -> {
            assertThat(card.word()).isEqualTo("вода");
            assertThat(card.pictogramUrl()).isNull();
        });
        assertThat(lesson.quiz()).singleElement().satisfies(question -> {
            assertThat(question.options()).containsExactly("Солнце", "Луна", "Ветер");
            assertThat(question.correctIndex()).isZero();
        });
    }

    @Test
    void invalidJsonContentIsReportedAsInvalidResponse() {
        respondWith(completion("{\"sentences\": [", null, "stop"));

        assertThatThrownBy(() -> adapt()).isInstanceOf(LlmInvalidResponseException.class);
    }

    @Test
    void refusalIsReportedAsInvalidResponse() {
        respondWith(completion(null, "I cannot help with that.", "stop"));

        assertThatThrownBy(() -> adapt())
                .isInstanceOf(LlmInvalidResponseException.class)
                .hasMessageContaining("refused");
    }

    @Test
    void truncatedResponseIsReportedAsInvalidResponse() {
        respondWith(completion("{\"sentences\": [", null, "length"));

        assertThatThrownBy(() -> adapt())
                .isInstanceOf(LlmInvalidResponseException.class)
                .hasMessageContaining("truncated");
    }

    @Test
    void httpErrorIsReportedAsLlmException() {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> adapt())
                .isInstanceOf(LlmException.class)
                .isNotInstanceOf(LlmInvalidResponseException.class)
                .hasMessageContaining("401");
    }

    private AdaptedLesson adapt() {
        return client.adapt("Урок", "Текст урока.", AdaptationProfile.DYSLEXIA);
    }

    private void respondWith(String body) {
        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private String completion(String content, String refusal, String finishReason) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("content", content);
        message.put("refusal", refusal);
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "id", "chatcmpl-test",
                    "object", "chat.completion",
                    "choices", List.of(Map.of(
                            "index", 0,
                            "message", message,
                            "finish_reason", finishReason))));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
