# hack-db0dd8bc-kto-mi

[![CI](https://github.com/BAITC-Hacks/hack-db0dd8bc-kto-mi/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/BAITC-Hacks/hack-db0dd8bc-kto-mi/actions/workflows/ci.yml)

Hackathon team repository for Kto mi?

## Qadam

Сервис адаптации учебных материалов для детей с особенностями развития (дислексия, аутизм).
Учитель вставляет текст урока и выбирает профиль, LLM возвращает адаптированный текст,
карточки с пиктограммами и простой тест. Учитель проверяет и утверждает материал,
после чего ребёнок видит его в простом интерфейсе.

**Стек:** Java 21, Spring Boot 3, Maven, H2, Swagger UI.

### Быстрый старт

По умолчанию приложение работает в mock-режиме: LLM не вызывается, ключ OpenAI не нужен.

**Вариант 1. Docker Compose** (нужен только Docker):

```bash
docker compose up --build
```

Контейнер считается здоровым, когда отвечает `GET /api/health`. Остановить: `docker compose down`.

**Вариант 2. Maven Wrapper** (нужен JDK 21 или новее):

```powershell
.\mvnw.cmd spring-boot:run      # Windows
```

```bash
./mvnw spring-boot:run          # Linux / macOS
```

После запуска:

- Health check: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui.html

**Режим OpenAI.** Скопируйте `.env.example` в `.env` и укажите:

```dotenv
LLM_MODE=openai
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
```

`docker compose` читает `.env` автоматически. При запуске через Maven задайте те же переменные
в окружении (например, `$env:LLM_MODE="openai"; $env:OPENAI_API_KEY="sk-..."`).
Без ключа в режиме `openai` приложение не стартует. `.env` в git не коммитится.

Сборка и тесты: `.\mvnw.cmd clean verify`. Тесты работают без сети, отчёт о покрытии —
`target/site/jacoco/index.html`.

При старте в пустую базу добавляются два утверждённых демо-урока: «Круговорот воды в природе» (дислексия)
и «Части растения» (аутизм). Отключить: `$env:DEMO_DATA_ENABLED="false"`.

### API

| Метод и адрес                       | Что делает                                                                  |
|-------------------------------------|-----------------------------------------------------------------------------|
| `POST /api/lessons`                 | создать урок `{title, text, profile}` и адаптировать его; статус `DRAFT`    |
| `GET /api/lessons`                  | список уроков (id, название, профиль, статус, дата создания), новые сверху  |
| `GET /api/lessons/{id}`             | урок целиком: исходный текст и адаптированный контент                       |
| `PUT /api/lessons/{id}/content`     | учитель правит адаптированный контент; статус снова `DRAFT`                 |
| `POST /api/lessons/{id}/approve`    | утвердить урок (`APPROVED`)                                                 |
| `POST /api/lessons/{id}/regenerate` | адаптировать урок заново; статус `DRAFT`                                    |
| `DELETE /api/lessons/{id}`          | удалить урок                                                                |
| `GET /api/lessons/{id}/student`     | версия для ребёнка: название, контент и настройки отображения; 403, если урок не утверждён |
| `GET /api/profiles`                 | профили адаптации: код, название, настройки отображения                     |

Ограничения: название 3–120 символов, текст 50–5000 символов, профиль обязателен (`DYSLEXIA` или `AUTISM`).

Ошибки возвращаются в едином формате с сообщением на русском:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Урок с id=42 не найден",
  "timestamp": "2026-09-23T10:15:30Z"
}
```

Коды: 400 — ошибка валидации (с полем `fieldErrors`), 403 — урок не утверждён, 404 — урок не найден,
502 — не удалось адаптировать текст с помощью LLM.

## Credits

Пиктограммы: [ARASAAC](https://arasaac.org), лицензия Creative Commons BY-NC-SA
(только некоммерческое использование, с указанием автора и источника, производные работы — под той же лицензией).

Официальный текст атрибуции, который фронтенд показывает в футере:

> The pictographic symbols used are the property of the Government of Aragon and have been created
> by Sergio Palao for ARASAAC (https://arasaac.org) which distributes them under a Creative Commons
> license (BY-NC-SA).

Автор пиктограмм: Sergio Palao. Правообладатель: Government of Aragon.
Условия использования: https://arasaac.org/terms-of-use
