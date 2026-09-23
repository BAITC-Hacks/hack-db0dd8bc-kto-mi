package com.qadam.llm.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Request body of the OpenAI Chat Completions API, limited to the fields Qadam uses.
 */
record ChatCompletionRequest(
        String model,
        List<Message> messages,
        @JsonProperty("response_format") ResponseFormat responseFormat
) {

    record Message(String role, String content) {
    }

    record ResponseFormat(
            String type,
            @JsonProperty("json_schema") JsonSchema jsonSchema
    ) {
    }

    record JsonSchema(String name, boolean strict, JsonNode schema) {
    }
}
