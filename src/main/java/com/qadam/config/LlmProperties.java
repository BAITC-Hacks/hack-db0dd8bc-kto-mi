package com.qadam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * LLM settings bound from {@code qadam.llm.*}.
 *
 * @param mode   which {@link com.qadam.llm.LlmClient} implementation to use
 * @param openai settings used only when {@code mode=openai}
 */
@ConfigurationProperties("qadam.llm")
public record LlmProperties(
        @DefaultValue("mock") Mode mode,
        @DefaultValue OpenAi openai
) {

    public enum Mode {
        MOCK,
        OPENAI
    }

    /**
     * @param apiKey  OpenAI API key, taken from the {@code OPENAI_API_KEY} environment variable
     * @param model   chat model name, e.g. {@code gpt-4o-mini}
     * @param timeout connect and read timeout of a single API call
     * @param baseUrl OpenAI API base URL
     */
    public record OpenAi(
            String apiKey,
            @DefaultValue("gpt-4o-mini") String model,
            @DefaultValue("60s") Duration timeout,
            @DefaultValue("https://api.openai.com/v1") String baseUrl
    ) {
    }
}
