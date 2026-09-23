package com.qadam.controller;

import com.qadam.service.DemoLessons;

/**
 * Request and error examples shown in Swagger UI.
 */
final class ApiExamples {

    static final String CREATE_WATER_CYCLE = "{\"title\": \"" + DemoLessons.WATER_CYCLE_TITLE + "\", "
            + "\"text\": \"" + DemoLessons.WATER_CYCLE_TEXT + "\", "
            + "\"profile\": \"DYSLEXIA\"}";

    static final String CREATE_PLANT_PARTS = "{\"title\": \"" + DemoLessons.PLANT_PARTS_TITLE + "\", "
            + "\"text\": \"" + DemoLessons.PLANT_PARTS_TEXT + "\", "
            + "\"profile\": \"AUTISM\"}";

    static final String UPDATE_CONTENT = """
            {
              "sentences": [
                { "text": "Вода на Земле всё время движется по кругу.", "keywords": ["Вода", "движется"], "section": null },
                { "text": "Солнце нагревает воду в морях и реках.", "keywords": ["Солнце"], "section": null },
                { "text": "Тёплая вода превращается в пар.", "keywords": ["пар"], "section": null },
                { "text": "Из пара получаются облака.", "keywords": ["облака"], "section": null },
                { "text": "Из облаков идёт дождь.", "keywords": ["дождь"], "section": null }
              ],
              "cards": [
                { "word": "пар", "explanation": "Пар получается из тёплой воды и поднимается вверх.", "pictogramUrl": null },
                { "word": "облако", "explanation": "Облако — это много капель воды в небе.", "pictogramUrl": null }
              ],
              "quiz": [
                { "question": "Что нагревает воду?", "options": ["Луна", "Солнце", "Ветер"], "correctIndex": 1 }
              ]
            }
            """;

    static final String VALIDATION_ERROR = """
            {
              "status": 400,
              "error": "Bad Request",
              "message": "Проверьте правильность заполнения полей",
              "timestamp": "2026-09-23T10:15:30Z",
              "fieldErrors": [
                { "field": "text", "message": "Текст урока должен содержать от 50 до 5000 символов" },
                { "field": "profile", "message": "Выберите профиль адаптации" }
              ]
            }
            """;

    static final String NOT_FOUND = """
            {
              "status": 404,
              "error": "Not Found",
              "message": "Урок с id=42 не найден",
              "timestamp": "2026-09-23T10:15:30Z"
            }
            """;

    static final String NOT_APPROVED = """
            {
              "status": 403,
              "error": "Forbidden",
              "message": "Урок ещё не утверждён учителем и пока недоступен ученику",
              "timestamp": "2026-09-23T10:15:30Z"
            }
            """;

    static final String LLM_FAILURE = """
            {
              "status": 502,
              "error": "Bad Gateway",
              "message": "Не удалось адаптировать текст урока. Попробуйте ещё раз чуть позже",
              "timestamp": "2026-09-23T10:15:30Z"
            }
            """;

    private ApiExamples() {
    }
}
