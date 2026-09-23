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
                .description("Qadam — платформа бизнес-задач для студентов. "
                        + "Бизнес описывает задачу своими словами, ИИ задаёт уточняющие вопросы и собирает карточку "
                        + "(без выдуманных фактов), прозрачный рейтинг показывает, чего не хватает. "
                        + "Бизнес вручную публикует задачу, студенческие команды откликаются, "
                        + "бизнес принимает отклики и подтверждает этапы работы."));
    }
}
