# Qadam

Hackathon service that adapts learning materials for children with developmental
differences (dyslexia, autism).

Flow:
1. A teacher pastes lesson text and picks a profile (e.g. dyslexia, autism).
2. An LLM returns an adapted text, cards with pictograms and a simple quiz.
3. The teacher reviews and approves the material.
4. The child sees the approved material in a simple interface.

## Project name
- The project is called **Qadam** (Kazakh for "step"): a child learns step by step, at their own pace.
- Hackathon team: «Kto mi?». The repository is named `hack-db0dd8bc-kto-mi`, but the UI,
  README and all documentation always use the name Qadam.
- Slogan: «Qadam — каждый урок понятен каждому ребёнку».

## Stack
- Java 21, Spring Boot 3.5, Maven (via Maven Wrapper)
- Spring Web, Validation, Data JPA, H2 (in-memory)
- Lombok
- springdoc-openapi (Swagger UI at `/swagger-ui.html`)
- JUnit 5, Spring Boot Test

## Package structure (`com.qadam`)
| Package      | Purpose                                                      |
|--------------|--------------------------------------------------------------|
| `config`     | Spring configuration, typed properties, OpenAPI, CORS        |
| `controller` | REST controllers (`/api/**`)                                  |
| `dto`        | Request/response objects (prefer Java records)               |
| `model`      | JPA entities                                                 |
| `repository` | Spring Data JPA repositories                                 |
| `service`    | Business logic (adaptation workflow, teacher approval)       |
| `llm`        | `LlmClient` abstraction, prompt; `llm.mock` and `llm.openai` implementations |
| `pictogram`  | ARASAAC pictogram lookup (`PictogramService`) and card enrichment (`PictogramEnricher`) |

## Commands (Windows / PowerShell)
```powershell
.\mvnw.cmd clean verify        # build + tests
.\mvnw.cmd spring-boot:run     # run on http://localhost:8080
```
- Health check: `GET /api/health` → `{"status":"ok"}`
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:qadam`)

JDK: the build targets Java 21 (`maven.compiler.release=21`) and works with any JDK >= 21.
On this machine only JDK 25 is installed, so set JAVA_HOME for the session:
`$env:JAVA_HOME="$env:USERPROFILE\.jdks\openjdk-25.0.1"`.

## Configuration
- `LLM_MODE` → `qadam.llm.mode`: `mock` (default, no network) or `openai`
- `OPENAI_API_KEY` — required only when `LLM_MODE=openai`; the app refuses to start without it
- `OPENAI_MODEL` — OpenAI chat model, default `gpt-4o-mini` (must support structured output)
- `PICTOGRAMS_ENABLED` → `qadam.pictograms.enabled`: `true` (default) or `false` to skip all ARASAAC requests
  (offline, tests)
- `DEMO_DATA_ENABLED` → `qadam.demo-data.enabled`: `true` (default) creates two approved demo lessons
  on startup if the DB is empty (`DemoDataLoader`, via the regular `LessonService`; texts in `DemoLessons`)
- `spring.web.locale=ru` with a fixed locale resolver: default Bean Validation messages are always Russian.
- Tests: `src/test/resources/application.properties` forces `mock` LLM, pictograms off and demo data off.
  All Spring test contexts share `jdbc:h2:mem:qadam`, so tests clean the repository in `@BeforeEach`.
- See `.env.example`. Local overrides go to `.env` / `application-local.yml` (both git-ignored).

## REST API
`LessonController` / `ProfileController` delegate to `LessonService` (lesson workflow; returns DTOs only).
| Method & path                      | Result                                                                  |
|------------------------------------|-------------------------------------------------------------------------|
| `POST /api/lessons`                | `{title, text, profile}` → adapt (LLM + pictograms) → 201, `DRAFT`       |
| `GET /api/lessons`                 | summaries `{id, title, profile, status, createdAt}`, newest first        |
| `GET /api/lessons/{id}`            | full lesson with original text and adapted `content`                    |
| `PUT /api/lessons/{id}/content`    | body `AdaptedLesson`; replaces content, status → `DRAFT`                 |
| `POST /api/lessons/{id}/approve`   | status → `APPROVED`                                                     |
| `POST /api/lessons/{id}/regenerate`| adapt the original text again, status → `DRAFT`                         |
| `DELETE /api/lessons/{id}`         | 204                                                                     |
| `GET /api/lessons/{id}/student`    | `{title, displaySettings, content}`; 403 unless `APPROVED`              |
| `GET /api/profiles`                | `{code, name, displaySettings}` per `AdaptationProfile`                 |
- Validation: title 3–120, text 50–5000 chars, profile required (Russian messages on `CreateLessonRequest`).
- The LLM call runs outside DB transactions; if adaptation fails nothing is saved/changed.
- Errors (`GlobalExceptionHandler`, extends `ResponseEntityExceptionHandler`): body
  `{status, error, message, timestamp, fieldErrors?}`, `message` in Russian. 400 validation / bad JSON / bad id
  (with `fieldErrors`), 403 `LessonNotApprovedException`, 404 `LessonNotFoundException`,
  502 `LlmAdaptationException`, 500 anything else. Standard MVC errors (405, 415, …) use the same format.
- Swagger examples live in `controller/ApiExamples`; API info in `config/OpenApiConfig`.

## LLM layer
- `LessonAdaptationService` calls `LlmClient`, validates the `AdaptedLesson` with Bean Validation
  and retries once on an unparseable/invalid result, then throws `LlmAdaptationException`.
- `OpenAiLlmClient` uses Chat Completions via `RestClient` (no SDK) with
  `response_format: json_schema, strict: true`; timeout 60s (`qadam.llm.openai.timeout`).
- Resources:
  - `prompts/adapt-lesson.md` — system prompt; `{{profileName}}` / `{{profileRules}}` come from `AdaptationProfile`.
  - `llm/adapted-lesson.schema.json` — strict JSON schema; keep it in sync with the DTO records
    (`AdaptedLessonSchemaTest` checks this).
  - `mock/<lesson>-<profile>.json` — prepared adaptations for the mock mode
    («Круговорот воды в природе», «Части растения», and a `demo` stub for any other title).

## Pictograms (ARASAAC)
- `LessonAdaptationService` passes the validated lesson to `PictogramEnricher`, which sets
  `Card.pictogramUrl` for all cards in parallel (virtual threads); not found → `null`.
- `PictogramService.findPictogramUrl(word)` normalizes the word (trim, edge punctuation, lower case),
  calls `GET https://api.arasaac.org/v1/pictograms/ru/search/{word}` and takes the first result's `_id`.
  Image URL: `https://static.arasaac.org/pictograms/{id}/{id}_500.png`.
  An unknown word returns HTTP 404 with `[]`.
- In-memory cache per normalized word, including "not found". Network and 5xx errors are not cached.
  Timeout 5s (`qadam.pictograms.timeout`). The service never throws; failures are logged as warnings.
- License: ARASAAC pictograms are CC BY-NC-SA (non-commercial, share-alike).
  **The frontend must show the ARASAAC attribution (README → Credits) in the footer of every page
  that displays pictograms.**

## Code rules
- English only in code, comments, identifiers, commit messages and PR descriptions.
- Commits follow Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `build:`.
- Secrets only via environment variables. Never commit keys, tokens or `.env`.
- Every new service gets a unit test.
- Controllers stay thin: validate input (`@Valid`), delegate to services, return DTOs, never entities.
