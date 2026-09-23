# Qadam

Hackathon service that adapts learning materials for children with developmental
differences (dyslexia, autism).

Flow:
1. A teacher pastes lesson text and picks a profile (e.g. dyslexia, autism).
2. An LLM returns an adapted text, cards with pictograms and a simple quiz.
3. The teacher reviews and approves the material.
4. The child sees the approved material in a simple interface.

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
| `llm`        | LLM client abstraction: `mock` and `openai` implementations  |
| `pictogram`  | Pictogram lookup for cards                                   |

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
- `LLM_MODE` — `mock` (default, no network) or `openai`
- `OPENAI_API_KEY` — required only when `LLM_MODE=openai`
- See `.env.example`. Local overrides go to `.env` / `application-local.yml` (both git-ignored).

## Code rules
- English only in code, comments, identifiers, commit messages and PR descriptions.
- Commits follow Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `build:`.
- Secrets only via environment variables. Never commit keys, tokens or `.env`.
- Every new service gets a unit test.
- Controllers stay thin: validate input (`@Valid`), delegate to services, return DTOs, never entities.
