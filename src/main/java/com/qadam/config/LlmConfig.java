package com.qadam.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.llm.TaskPrompts;
import com.qadam.llm.LlmClient;
import com.qadam.llm.mock.MockLlmClient;
import com.qadam.llm.openai.OpenAiLlmClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Selects the {@link LlmClient} implementation from {@code qadam.llm.mode}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    public LlmClient llmClient(LlmProperties properties, ObjectMapper objectMapper,
                               RestClient.Builder restClientBuilder) {
        return switch (properties.mode()) {
            case MOCK -> new MockLlmClient();
            case OPENAI -> openAiClient(properties.openai(), objectMapper, restClientBuilder);
        };
    }

    private static OpenAiLlmClient openAiClient(LlmProperties.OpenAi openai, ObjectMapper objectMapper,
                                                RestClient.Builder restClientBuilder) {
        if (!StringUtils.hasText(openai.apiKey())) {
            throw new IllegalStateException(
                    "qadam.llm.mode=openai requires the OPENAI_API_KEY environment variable to be set");
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(openai.timeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(openai.timeout());

        RestClient restClient = restClientBuilder.clone()
                .baseUrl(openai.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openai.apiKey())
                .requestFactory(requestFactory)
                .build();
        return new OpenAiLlmClient(restClient, openai.model(), new TaskPrompts(), objectMapper);
    }
}
