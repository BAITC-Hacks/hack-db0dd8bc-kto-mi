# Qadam

Hackathon MVP: a platform of business tasks for student teams.

Flow:
1. A business describes a task in its own words (draft) and picks an industry.
2. The AI finds which card fields are missing and asks at least 3 clarifying questions.
3. The AI builds a task card strictly from the draft and the answers (no invented facts; unknown → empty field).
4. A transparent, deterministic rating (no AI) shows the card quality and what to fill in; the business edits the card.
5. The business publishes the task manually; it appears in the catalog.
6. Student teams get recommendations and send proposals; the business accepts/rejects them manually
   and confirms milestones (+10 points to the team).

## Project name
- The project is called **Qadam** (Kazakh for "step"): from a raw idea to a ready task step by step.
- Hackathon team: «Kto mi?». The repository is named `hack-db0dd8bc-kto-mi`, but the UI,
  README and all documentation always use the name Qadam.

## Stack
- Java 21, Spring Boot 3.5, Maven (via Maven Wrapper)
- Spring Web, Validation, Data JPA, H2 (in-memory)
- Lombok
- springdoc-openapi (Swagger UI at `/swagger-ui.html`)
- JUnit 5, Spring Boot Test

## Package structure (`com.qadam`)
| Package      | Purpose                                                                          |
|--------------|----------------------------------------------------------------------------------|
| `config`     | Spring configuration, typed properties, OpenAPI, demo data loader                 |
| `controller` | REST controllers (`/api/**`), `GlobalExceptionHandler`, Swagger examples          |
| `dto`        | Request/response records (`TaskCard`, `TaskAnalysis`, `Rating`, …)               |
| `model`      | JPA entities `Task`, `Team`, `Proposal`; enums `CardField`, `Industry`, `RatingLevel`, statuses |
| `repository` | Spring Data JPA repositories                                                     |
| `service`    | Business logic: `TaskService`, `TaskAiService`, `RatingService`, `TeamService`, `TaskMatcher`, `ProposalService`, `DemoData` |
| `llm`        | `LlmClient` abstraction, `TaskPrompts`; `llm.mock` and `llm.openai` implementations |

The frontend lives in `src/main/resources/static` and is owned by another team member — do not edit it.
Do not edit `AGENTS.md`.

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

## CI and Docker
- CI: `.github/workflows/ci.yml` (GitHub Actions) runs on push to `main` and on every pull request:
  Temurin JDK 21 with Maven cache, `./mvnw -B -ntp clean verify`, env `LLM_MODE=mock`,
  `DEMO_DATA_ENABLED=false` (tests must never need the network).
  The JaCoCo report is uploaded as the `jacoco-report` build artifact.
- Coverage: `jacoco-maven-plugin` in `pom.xml`, report at `target/site/jacoco/index.html` on `verify`.
  No coverage threshold — the build must not fail on coverage.
- `mvnw` must stay executable (git mode `100755`) with LF endings (`.gitattributes`).
- `Dockerfile` (multi-stage): `eclipse-temurin:21-jdk` builds the jar via the Maven Wrapper with
  `-DskipTests` (tests run in CI); runtime is `eclipse-temurin:21-jre-alpine` as non-root user `qadam`,
  port 8080, JVM flags via `JAVA_OPTS`. Keep `.dockerignore` up to date (no `target`, `.git`, `.idea`, `.env`).
- `docker-compose.yml`: service `qadam`, `8080:8080`, `LLM_MODE` (default `mock`), `OPENAI_API_KEY`,
  `OPENAI_MODEL`, `DEMO_DATA_ENABLED` — taken from `.env` if present; healthcheck `wget` on `/api/health`.
  ```bash
  docker compose up --build
  ```

## Configuration
- `LLM_MODE` → `qadam.llm.mode`: `mock` (default, no network) or `openai`
- `OPENAI_API_KEY` — required only when `LLM_MODE=openai`; the app refuses to start without it
- `OPENAI_MODEL` — OpenAI chat model, default `gpt-4o-mini` (must support structured output)
- `DEMO_DATA_ENABLED` → `qadam.demo-data.enabled`: `true` (default) fills an empty DB on startup
  (`DemoDataLoader`, content in `service/DemoData`): 5 drafts of different completeness, 5 published tasks
  covering all 4 rating levels, 5 teams, 5 proposals. No LLM calls. Synthetic Russian texts, no personal data
  (contacts use `example.com`).
- `spring.web.locale=ru` with a fixed locale resolver: default Bean Validation messages are always Russian.
- Tests: `src/test/resources/application.properties` forces `mock` LLM and demo data off.
  All Spring test contexts share `jdbc:h2:mem:qadam`, so tests clean the repositories in `@BeforeEach`.
- See `.env.example`. Local overrides go to `.env` / `application-local.yml` (both git-ignored).

## Domain
- `Task`: `id, industry, draftText, status (DRAFT|PUBLISHED), createdAt, updatedAt` + card fields
  `title, context, need, users, data, constraints, expectedResult, successCriteria, contact, interactionFormat`
  + stored `score`/`level` (recalculated on every card change, used for catalog sorting/filtering).
- `TaskCard` (dto record) is the card: trims values, `null` → `""`. `CardField` enum holds the JSON codes,
  Russian labels and the "filled meaningfully" rule. `CardField.CODE_PATTERN` must list all codes.
- `Team`: `id, name, interests, skills, technologies` (string lists stored as JSON), `points`.
- `Proposal`: `id, taskId, teamId, idea, plan, duration, prototypeUrl, status (PENDING|ACCEPTED|REJECTED),
  confirmedMilestones, createdAt`.
- `Industry` enum: API code = constant name, `displayName` in Russian (`GET /api/industries`).

## Rating (`RatingService`, no AI)
| Criterion              | Points | Rule                                                              |
|------------------------|--------|-------------------------------------------------------------------|
| Контекст и потребность | 20     | context 10 + need 10                                              |
| Данные и материалы     | 20     | data                                                              |
| Ожидаемый результат    | 15     | expectedResult                                                    |
| Критерии успеха        | 15     | 15 if it has a digit or `%`, otherwise 7 (half, rounded down)     |
| Ограничения            | 10     | constraints                                                       |
| Пользователи           | 10     | users                                                             |
| Связь с бизнесом       | 10     | contact 5 + interactionFormat 5                                   |
- A field counts if it is not blank and at least 15 characters (contact: 5).
- Levels (`RatingLevel`): `DRAFT` 0–39 «Черновик», `WORKING` 40–69 «Рабочая задача»,
  `READY` 70–89 «Готова к работе», `PRIORITY` 90–100 «Приоритетная».
- `Rating`: `{score, level, levelName, breakdown[{criterion, points, maxPoints, reason}], missing[field codes],
  tips[strings with points]}`. Every task response contains `rating`.

## REST API
Controllers stay thin and delegate to services. The frontend is built against these exact paths and fields.
| Method & path                               | Result                                                                  |
|---------------------------------------------|-------------------------------------------------------------------------|
| `POST /api/tasks/analyze`                   | `{draftText, industry}` → `{missingFields, questions[{field, question}]}`; nothing saved |
| `POST /api/tasks`                           | `{draftText, industry, answers[{field, answer}]}` → AI card → 201, `DRAFT` |
| `GET /api/tasks/{id}`                       | task: card fields at top level, `rating`, `needsClarification`, `clarificationNote` |
| `PUT /api/tasks/{id}/card`                  | body `TaskCard` (all fields replaced); rating recalculated; status unchanged |
| `POST /api/tasks/{id}/publish`              | manual publication → `PUBLISHED`                                        |
| `GET /api/catalog?industry=&level=`         | only `PUBLISHED`, score desc; `DRAFT`-level tasks have `needsClarification=true`, note «требует уточнения» |
| `GET /api/industries`                       | `[{code, name}]`                                                        |
| `GET /api/teams`                            | `[{id, name, interests, skills, technologies, points}]`                 |
| `GET /api/teams/{id}/recommendations`       | published tasks with level ≥ `WORKING`, sorted by word match (`TaskMatcher`): `[{task, matchScore, matchedKeywords}]` |
| `POST /api/tasks/{id}/proposals`            | `{teamId, idea, plan, duration, prototypeUrl}` → 201 `PENDING`; 409 unless task `PUBLISHED`; unlimited count |
| `GET /api/tasks/{id}/proposals`             | proposals in creation order, with `teamName`, `teamPoints`              |
| `POST /api/proposals/{id}/accept` / `reject`| manual business decision, only from `PENDING` (else 409); several or none may be accepted |
| `POST /api/proposals/{id}/confirm-milestone`| only `ACCEPTED` (else 409); team `points += 10`, `confirmedMilestones += 1` |
- Validation: `draftText` 20–5000, `industry` required, answers ≤ 20 with a known `field`;
  card fields ≤ 3000 (title ≤ 200, contact ≤ 300); proposal `idea` 10–2000, `plan` 10–4000, `duration` required,
  `prototypeUrl` optional `http(s)://`. Messages in Russian.
- Recommendations (`TaskMatcher`): team interests/skills/technologies vs. task text (industry name + card
  without contact/format); words are lower-cased, stop words dropped, stem = first 5 letters.
- The LLM call runs outside DB transactions; if the AI fails nothing is saved.
- Errors (`GlobalExceptionHandler`, extends `ResponseEntityExceptionHandler`): body
  `{status, error, message, timestamp, fieldErrors?}`, `message` in Russian. 400 validation / bad JSON / bad
  enum or id (with `fieldErrors`), 404 `NotFoundException`, 409 `InvalidStateException`,
  502 `LlmFailureException`, 500 anything else. Standard MVC errors (405, 415, …) use the same format.
- Swagger examples live in `controller/ApiExamples`; API info in `config/OpenApiConfig`.

## LLM layer
- `LlmClient` has two functions: `analyze(draftText, industry) → TaskAnalysis` and
  `buildCard(draftText, industry, answers) → TaskCard`.
- `TaskAiService` validates the result with Bean Validation (≥ 3 questions, known field codes, field lengths)
  and retries once on an unparseable/invalid result, then throws `LlmFailureException`. An unavailable LLM
  (`LlmException`) is not retried.
- `OpenAiLlmClient` uses Chat Completions via `RestClient` (no SDK) with
  `response_format: json_schema, strict: true`; timeout 60s (`qadam.llm.openai.timeout`).
- `MockLlmClient` (default): deterministic, offline. Splits the draft into sentences and assigns each to a card
  field by keywords (unmatched → `context`); title = first "need" sentence. Answers are appended to their field.
  Never invents text. `analyze` asks about unfilled fields, topping up to 3 questions with the weakest fields.
- Resources:
  - `prompts/analyze-task.md`, `prompts/build-card.md` — system prompts (English, answers in Russian).
  - `llm/task-analysis.schema.json`, `llm/task-card.schema.json` — strict JSON schemas; keep them in sync with
    the DTO records and `CardField` (`TaskSchemaTest` checks this).

## Code rules
- English only in code, comments, identifiers, commit messages and PR descriptions
  (user-facing messages and demo content are in Russian).
- Commits follow Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `build:`.
- Secrets only via environment variables. Never commit keys, tokens or `.env`.
- Every new service gets a unit test.
- Controllers stay thin: validate input (`@Valid`), delegate to services, return DTOs, never entities.
