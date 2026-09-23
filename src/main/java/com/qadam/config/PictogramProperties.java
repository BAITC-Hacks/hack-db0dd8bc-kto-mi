package com.qadam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Pictogram lookup settings bound from {@code qadam.pictograms.*}.
 *
 * @param enabled      whether cards are enriched with pictograms; {@code false} disables all external requests
 * @param locale       ARASAAC search language
 * @param timeout      connect and read timeout of a single ARASAAC call
 * @param apiBaseUrl   ARASAAC API base URL
 * @param imageBaseUrl base URL of ARASAAC pictogram images
 */
@ConfigurationProperties("qadam.pictograms")
public record PictogramProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("ru") String locale,
        @DefaultValue("5s") Duration timeout,
        @DefaultValue("https://api.arasaac.org/v1") String apiBaseUrl,
        @DefaultValue("https://static.arasaac.org/pictograms") String imageBaseUrl
) {
}
