package com.qadam.config;

import com.qadam.llm.LlmClient;
import com.qadam.llm.mock.MockLlmClient;
import com.qadam.llm.openai.OpenAiLlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LlmConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, RestClientAutoConfiguration.class))
            .withUserConfiguration(LlmConfig.class);

    @Test
    void usesMockClientByDefault() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(LlmClient.class).getBean(LlmClient.class)
                        .isInstanceOf(MockLlmClient.class));
    }

    @Test
    void usesOpenAiClientWhenModeIsOpenAiAndKeyIsSet() {
        contextRunner
                .withPropertyValues("qadam.llm.mode=openai", "qadam.llm.openai.api-key=test-key")
                .run(context -> assertThat(context).getBean(LlmClient.class).isInstanceOf(OpenAiLlmClient.class));
    }

    @Test
    void failsToStartWhenModeIsOpenAiWithoutKey() {
        contextRunner
                .withPropertyValues("qadam.llm.mode=openai", "qadam.llm.openai.api-key=")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("OPENAI_API_KEY"));
    }

    @Test
    void failsToStartOnUnknownMode() {
        contextRunner
                .withPropertyValues("qadam.llm.mode=gemini")
                .run(context -> assertThat(context).hasFailed());
    }
}
