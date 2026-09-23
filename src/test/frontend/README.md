# Qadam frontend

The frontend lives in `src/main/resources/static/` and uses only the existing
`/api/profiles` and `/api/lessons` endpoints. No frontend build or npm dependencies
are required. Spring Boot serves these static resources after the backend owner
builds/runs the application.

To preview frontend edits immediately against an already running backend without
rebuilding or modifying it:

```sh
node src/test/frontend/preview.mjs
```

Open http://127.0.0.1:8082. The development server forwards API requests to
http://127.0.0.1:8080. Override `QADAM_BACKEND_URL` or `QADAM_FRONTEND_PORT` if needed.
This server is for local development only and binds to loopback.

Run the frontend tests:

```sh
node --test src/test/frontend/*.test.mjs
```

The interface supports creation, review, editing, regeneration, approval, a shared
lesson library, and learner steps with vocabulary and quizzes. Learner display
settings come from the student endpoint. Progress is kept in the currently opened
lesson only; reopening or refreshing starts again. Missing pictograms keep their
text explanation, and displayed pictograms include ARASAAC attribution.

The original backend does not expose its AI/mock mode. The frontend does not
infer it or request a new endpoint. The backend owner configures the adaptation
provider. In the backend's default mock mode, responses are prepared examples,
not real adaptations of arbitrary source text. Review before approval remains
necessary in either mode.
