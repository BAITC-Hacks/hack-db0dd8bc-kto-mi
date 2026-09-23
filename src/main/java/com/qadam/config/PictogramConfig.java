package com.qadam.config;

import com.qadam.pictogram.PictogramService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Creates the {@link PictogramService} with a {@link RestClient} for the ARASAAC API.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PictogramProperties.class)
public class PictogramConfig {

    @Bean
    public PictogramService pictogramService(PictogramProperties properties, RestClient.Builder restClientBuilder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        RestClient restClient = restClientBuilder.clone()
                .baseUrl(properties.apiBaseUrl())
                .requestFactory(requestFactory)
                .build();
        return new PictogramService(restClient, properties);
    }
}
