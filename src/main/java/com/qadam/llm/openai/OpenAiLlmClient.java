package com.qadam.llm.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.dto.AdaptedLesson;
import com.qadam.llm.AdaptationPrompt;
import com.qadam.llm.LlmClient;
import com.qadam.llm.LlmException;
import com.qadam.llm.LlmInvalidResponseException;
import com.qadam.model.AdaptationProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Calls the OpenAI Chat Completions API with a strict JSON schema so the answer always
 * has the shape of {@link AdaptedLesson}.
 *
 * <p>The {@link RestClient} must already be configured with the base URL, authorization
 * header and timeouts.
 */
@Slf4j
public class OpenAiLlmClient implements LlmClient {

    static final String SCHEMA_PATH = "llm/adapted-lesson.schema.json";
    static final String SCHEMA_NAME = "adapted_lesson";

    private final RestClient restClient;
    private final String model;
    private final AdaptationPrompt prompt;
    private final ObjectMapper objectMapper;
    private final JsonNode schema;

    public OpenAiLlmClient(RestClient restClient, String model, AdaptationPrompt prompt, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.model = model;
        this.prompt = prompt;
        this.objectMapper = objectMapper;
        this.schema = readSchema(objectMapper);
    }

    @Override
    public AdaptedLesson adapt(String title, String text, AdaptationProfile profile) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatCompletionRequest.Message("system", prompt.systemPrompt(profile)),
                        new ChatCompletionRequest.Message("user", prompt.userPrompt(title, text))
                ),
                new ChatCompletionRequest.ResponseFormat(
                        "json_schema",
                        new ChatCompletionRequest.JsonSchema(SCHEMA_NAME, true, schema)
                )
        );
        return parse(send(request));
    }

    private ChatCompletionResponse send(ChatCompletionRequest request) {
        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (RestClientResponseException e) {
            throw new LlmException("OpenAI API returned HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new LlmException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }

    private AdaptedLesson parse(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LlmInvalidResponseException("OpenAI response has no choices");
        }
        ChatCompletionResponse.Choice choice = response.choices().getFirst();
        ChatCompletionResponse.Message message = choice.message();
        if (message == null) {
            throw new LlmInvalidResponseException("OpenAI response has no message");
        }
        if (message.refusal() != null) {
            throw new LlmInvalidResponseException("Model refused to adapt the lesson: " + message.refusal());
        }
        if ("length".equals(choice.finishReason())) {
            throw new LlmInvalidResponseException("OpenAI response was truncated (finish_reason=length)");
        }
        if (message.content() == null || message.content().isBlank()) {
            throw new LlmInvalidResponseException("OpenAI response content is empty");
        }
        try {
            return objectMapper.readValue(message.content(), AdaptedLesson.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse adapted lesson from OpenAI response: {}", e.getOriginalMessage());
            throw new LlmInvalidResponseException("OpenAI response is not a valid adapted lesson JSON", e);
        }
    }

    private static JsonNode readSchema(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(SCHEMA_PATH).getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read JSON schema " + SCHEMA_PATH, e);
        }
    }
}
