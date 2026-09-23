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
