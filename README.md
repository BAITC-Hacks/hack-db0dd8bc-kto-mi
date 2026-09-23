# hack-db0dd8bc-kto-mi
Hackathon team repository for Kto mi?

## Qadam

Сервис адаптации учебных материалов для детей с особенностями развития (дислексия, аутизм).
Учитель вставляет текст урока и выбирает профиль, LLM возвращает адаптированный текст,
карточки с пиктограммами и простой тест. Учитель проверяет и утверждает материал,
после чего ребёнок видит его в простом интерфейсе.

**Стек:** Java 21, Spring Boot 3, Maven, H2, Swagger UI.

### Запуск (Windows)

```powershell
$env:LLM_MODE="mock"         # или "openai" + $env:OPENAI_API_KEY="..." (см. .env.example)
.\mvnw.cmd spring-boot:run
```

- Health check: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui.html

## Credits

Пиктограммы: [ARASAAC](https://arasaac.org), лицензия Creative Commons BY-NC-SA
(только некоммерческое использование, с указанием автора и источника, производные работы — под той же лицензией).

Официальный текст атрибуции, который фронтенд показывает в футере:

> The pictographic symbols used are the property of the Government of Aragon and have been created
> by Sergio Palao for ARASAAC (https://arasaac.org) which distributes them under a Creative Commons
> license (BY-NC-SA).

Автор пиктограмм: Sergio Palao. Правообладатель: Government of Aragon.
Условия использования: https://arasaac.org/terms-of-use
