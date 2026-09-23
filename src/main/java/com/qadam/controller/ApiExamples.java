package com.qadam.controller;

/**
 * Request and error examples shown in Swagger UI.
 */
final class ApiExamples {

    static final String ANALYZE = """
            {
              "draftText": "Мы сеть из 12 кофеен в Алматы. Хотим понять, почему в будни после обеда падают продажи. Есть выгрузка чеков из кассовой системы за 2 года в Excel.",
              "industry": "HORECA"
            }
            """;

    static final String CREATE = """
            {
              "draftText": "Мы сеть из 12 кофеен в Алматы. Хотим понять, почему в будни после обеда падают продажи. Есть выгрузка чеков из кассовой системы за 2 года в Excel.",
              "industry": "HORECA",
              "answers": [
                { "field": "expectedResult", "answer": "Дашборд с продажами по часам и рекомендации по акциям." },
                { "field": "successCriteria", "answer": "Рост дневной выручки после обеда на 10% за 2 месяца." },
                { "field": "contact", "answer": "Операционный менеджер, ops@example.com" }
              ]
            }
            """;

    static final String UPDATE_CARD = """
            {
              "title": "Почему падают продажи кофеен после обеда",
              "context": "Мы сеть из 12 кофеен в Алматы.",
              "need": "Хотим понять, почему в будни после обеда падают продажи.",
              "users": "Управляющие кофейнями и маркетолог сети.",
              "data": "Выгрузка чеков из кассовой системы за 2 года в Excel.",
              "constraints": "Персональные данные гостей не передаются, срок — 6 недель.",
              "expectedResult": "Дашборд с продажами по часам и рекомендации по акциям.",
              "successCriteria": "Рост дневной выручки после обеда на 10% за 2 месяца.",
              "contact": "Операционный менеджер, ops@example.com",
              "interactionFormat": "Онлайн-встреча раз в неделю и общий чат."
            }
            """;

    static final String CREATE_PROPOSAL = """
            {
              "teamId": 1,
              "idea": "Построим дашборд продаж по часам и проверим гипотезы о причинах спада.",
              "plan": "1. Очистка данных. 2. Анализ по часам и дням недели. 3. Дашборд. 4. Рекомендации.",
              "duration": "5 недель",
              "prototypeUrl": "https://example.com/prototype"
            }
            """;

    static final String VALIDATION_ERROR = """
            {
              "status": 400,
              "error": "Bad Request",
              "message": "Проверьте правильность заполнения полей",
              "timestamp": "2026-09-23T10:15:30Z",
              "fieldErrors": [
                { "field": "draftText", "message": "Черновик должен быть от 20 до 5000 символов" },
                { "field": "industry", "message": "Выберите отрасль" }
              ]
            }
            """;

    static final String NOT_FOUND = """
            {
              "status": 404,
              "error": "Not Found",
              "message": "Задача с id=42 не найдена",
              "timestamp": "2026-09-23T10:15:30Z"
            }
            """;

    static final String NOT_PUBLISHED = """
            {
              "status": 409,
              "error": "Conflict",
              "message": "Откликнуться можно только на опубликованную задачу",
              "timestamp": "2026-09-23T10:15:30Z"
            }
            """;

    static final String LLM_FAILURE = """
            {
              "status": 502,
              "error": "Bad Gateway",
              "message": "Не удалось обработать задачу с помощью ИИ. Попробуйте ещё раз чуть позже",
              "timestamp": "2026-09-23T10:15:30Z"
            }
            """;

    private ApiExamples() {
    }
}
