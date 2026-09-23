package com.qadam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General information shown at the top of Swagger UI.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI qadamOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Qadam API")
                .version("v1")
                .description("Qadam — каждый урок понятен каждому ребёнку. "
                        + "Учитель создаёт урок, сервис адаптирует его под профиль (дислексия, аутизм), "
                        + "учитель проверяет и утверждает материал, ребёнок видит утверждённый урок. "
                        + "Пиктограммы: ARASAAC (https://arasaac.org), лицензия CC BY-NC-SA."));
    }
}
