package com.qadam.llm.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.dto.FieldAnswer;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.llm.LlmClient;
import com.qadam.llm.LlmException;
import com.qadam.llm.LlmInvalidResponseException;
import com.qadam.llm.TaskPrompts;
import com.qadam.model.Industry;
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
 * has the shape of {@link TaskAnalysis} or {@link TaskCard}.
 *
 * <p>The {@link RestClient} must already be configured with the base URL, authorization
 * header and timeouts.
 */
@Slf4j
public class OpenAiLlmClient implements LlmClient {

    static final String ANALYSIS_SCHEMA_PATH = "llm/task-analysis.schema.json";
    static final String CARD_SCHEMA_PATH = "llm/task-card.schema.json";
    static final String ANALYSIS_SCHEMA_NAME = "task_analysis";
    static final String CARD_SCHEMA_NAME = "task_card";

    private final RestClient restClient;
    private final String model;
    private final TaskPrompts prompts;
    private final ObjectMapper objectMapper;
    private final JsonNode analysisSchema;
    private final JsonNode cardSchema;

    public OpenAiLlmClient(RestClient restClient, String model, TaskPrompts prompts, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.model = model;
        this.prompts = prompts;
        this.objectMapper = objectMapper;
        this.analysisSchema = readSchema(objectMapper, ANALYSIS_SCHEMA_PATH);
        this.cardSchema = readSchema(objectMapper, CARD_SCHEMA_PATH);
    }

    @Override
    public TaskAnalysis analyze(String draftText, Industry industry) {
        return complete(prompts.analyzeSystemPrompt(), prompts.analyzeUserPrompt(draftText, industry),
                ANALYSIS_SCHEMA_NAME, analysisSchema, TaskAnalysis.class);
    }

    @Override
    public TaskCard buildCard(String draftText, Industry industry, List<FieldAnswer> answers) {
        return complete(prompts.buildCardSystemPrompt(), prompts.buildCardUserPrompt(draftText, industry, answers),
                CARD_SCHEMA_NAME, cardSchema, TaskCard.class);
    }

    private <T> T complete(String systemPrompt, String userPrompt, String schemaName, JsonNode schema,
                           Class<T> resultType) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatCompletionRequest.Message("system", systemPrompt),
                        new ChatCompletionRequest.Message("user", userPrompt)
                ),
                new ChatCompletionRequest.ResponseFormat(
                        "json_schema",
                        new ChatCompletionRequest.JsonSchema(schemaName, true, schema)
                )
        );
        return parse(send(request), resultType);
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

    private <T> T parse(ChatCompletionResponse response, Class<T> resultType) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LlmInvalidResponseException("OpenAI response has no choices");
        }
        ChatCompletionResponse.Choice choice = response.choices().getFirst();
        ChatCompletionResponse.Message message = choice.message();
        if (message == null) {
            throw new LlmInvalidResponseException("OpenAI response has no message");
        }
        if (message.refusal() != null) {
            throw new LlmInvalidResponseException("Model refused to answer: " + message.refusal());
        }
        if ("length".equals(choice.finishReason())) {
            throw new LlmInvalidResponseException("OpenAI response was truncated (finish_reason=length)");
        }
        if (message.content() == null || message.content().isBlank()) {
            throw new LlmInvalidResponseException("OpenAI response content is empty");
        }
        try {
            return objectMapper.readValue(message.content(), resultType);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse {} from OpenAI response: {}", resultType.getSimpleName(), e.getOriginalMessage());
            throw new LlmInvalidResponseException(
                    "OpenAI response is not a valid " + resultType.getSimpleName() + " JSON", e);
        }
    }

    private static JsonNode readSchema(ObjectMapper objectMapper, String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read JSON schema " + path, e);
        }
    }
}
