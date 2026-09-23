package com.qadam.llm.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body of the OpenAI Chat Completions API, limited to the fields Qadam reads.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ChatCompletionResponse(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(
            Message message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    /**
     * @param refusal set instead of {@code content} when the model refuses to answer
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content, String refusal) {
    }
}
