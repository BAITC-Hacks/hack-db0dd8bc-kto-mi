package com.qadam.config;

import com.qadam.pictogram.PictogramService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PictogramConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(PictogramConfig.class);

    @Test
    void enabledByDefaultWithArasaacDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PictogramService.class);
            assertThat(context.getBean(PictogramService.class).isEnabled()).isTrue();

            PictogramProperties properties = context.getBean(PictogramProperties.class);
            assertThat(properties.locale()).isEqualTo("ru");
            assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.apiBaseUrl()).isEqualTo("https://api.arasaac.org/v1");
            assertThat(properties.imageBaseUrl()).isEqualTo("https://static.arasaac.org/pictograms");
        });
    }

    @Test
    void canBeDisabled() {
        contextRunner
                .withPropertyValues("qadam.pictograms.enabled=false")
                .run(context -> assertThat(context.getBean(PictogramService.class).isEnabled()).isFalse());
    }
}
