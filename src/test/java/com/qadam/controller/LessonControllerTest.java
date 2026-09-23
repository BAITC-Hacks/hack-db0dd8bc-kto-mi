package com.qadam.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.CreateLessonRequest;
import com.qadam.dto.Question;
import com.qadam.model.AdaptationProfile;
import com.qadam.repository.LessonRepository;
import com.qadam.service.DemoLessons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static com.qadam.dto.AdaptedLessonFixtures.validLesson;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lesson API over the mock LLM with pictograms disabled (see test {@code application.properties}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LessonControllerTest {

    private static final long UNKNOWN_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LessonRepository lessonRepository;

    @BeforeEach
    void cleanDatabase() {
        lessonRepository.deleteAll();
    }

    @Test
    void fullTeacherToStudentScenario() throws Exception {
        String response = mockMvc.perform(json(post("/api/lessons"), waterCycleRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/lessons/")))
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.title").value(DemoLessons.WATER_CYCLE_TITLE))
                .andExpect(jsonPath("$.originalText").value(DemoLessons.WATER_CYCLE_TEXT))
                .andExpect(jsonPath("$.profile").value("DYSLEXIA"))
                .andExpect(jsonPath("$.profileName").value("Дислексия"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.content.sentences[0].text")
                        .value("Вода на Земле всё время движется по кругу."))
                .andExpect(jsonPath("$.content.cards[0].pictogramUrl").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/lessons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.content.quiz").isNotEmpty());

        mockMvc.perform(json(put("/api/lessons/{id}/content", id), validLesson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.content.sentences", hasSize(3)))
                .andExpect(jsonPath("$.content.cards[0].word").value("seed"));

        mockMvc.perform(post("/api/lessons/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/lessons/{id}/student", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(DemoLessons.WATER_CYCLE_TITLE))
                .andExpect(jsonPath("$.displaySettings.fontSizePx").value(20))
                .andExpect(jsonPath("$.displaySettings.dyslexiaFont").value(true))
                .andExpect(jsonPath("$.content.sentences[0].text").value("First, the seed goes into the soil."))
                .andExpect(jsonPath("$.content.cards[0].pictogramUrl").value("https://example.org/pictograms/seed.png"))
                .andExpect(jsonPath("$.originalText").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void studentViewOfDraftIsForbidden() throws Exception {
        long id = createLesson(waterCycleRequest());

        mockMvc.perform(get("/api/lessons/{id}/student", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Урок ещё не утверждён учителем и пока недоступен ученику"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void editingApprovedLessonReturnsItToDraft() throws Exception {
        long id = createLesson(waterCycleRequest());
        mockMvc.perform(post("/api/lessons/{id}/approve", id)).andExpect(status().isOk());

        mockMvc.perform(json(put("/api/lessons/{id}/content", id), validLesson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/lessons/{id}/student", id)).andExpect(status().isForbidden());
    }

    @Test
    void regenerateReplacesTeacherEditsAndReturnsToDraft() throws Exception {
        long id = createLesson(waterCycleRequest());
        mockMvc.perform(json(put("/api/lessons/{id}/content", id), validLesson())).andExpect(status().isOk());
        mockMvc.perform(post("/api/lessons/{id}/approve", id)).andExpect(status().isOk());

        mockMvc.perform(post("/api/lessons/{id}/regenerate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.content.sentences[0].text")
                        .value("Вода на Земле всё время движется по кругу."));
    }

    @Test
    void listsLessonsNewestFirstWithoutTexts() throws Exception {
        long first = createLesson(waterCycleRequest());
        long second = createLesson(new CreateLessonRequest(
                DemoLessons.PLANT_PARTS_TITLE, DemoLessons.PLANT_PARTS_TEXT, AdaptationProfile.AUTISM));

        mockMvc.perform(get("/api/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[0].title").value(DemoLessons.PLANT_PARTS_TITLE))
                .andExpect(jsonPath("$[0].profile").value("AUTISM"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].createdAt").value(notNullValue()))
                .andExpect(jsonPath("$[0].originalText").doesNotExist())
                .andExpect(jsonPath("$[0].content").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(first));
    }

    @Test
    void deletesLesson() throws Exception {
        long id = createLesson(waterCycleRequest());

        mockMvc.perform(delete("/api/lessons/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lessons/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/lessons")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listsProfiles() throws Exception {
        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("DYSLEXIA", "AUTISM")))
                .andExpect(jsonPath("$[?(@.code == 'AUTISM')].name").value("Аутизм (РАС)"))
                .andExpect(jsonPath("$[?(@.code == 'AUTISM')].displaySettings.showSequenceStructure").value(true))
                .andExpect(jsonPath("$[?(@.code == 'DYSLEXIA')].displaySettings.backgroundColor").value("#FDF6E3"));
    }

    @Test
    void createReturnsFieldErrorsForInvalidRequest() throws Exception {
        mockMvc.perform(json(post("/api/lessons"), new CreateLessonRequest("Ур", "Слишком короткий текст.", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Проверьте правильность заполнения полей"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("title", "text", "profile")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')].message")
                        .value("Название урока должно содержать от 3 до 120 символов"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'text')].message")
                        .value("Текст урока должен содержать от 50 до 5000 символов"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'profile')].message")
                        .value("Выберите профиль адаптации"));

        mockMvc.perform(get("/api/lessons")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createRejectsTooLongTitleAndText() throws Exception {
        CreateLessonRequest request = new CreateLessonRequest(
                "а".repeat(121), "б".repeat(5001), AdaptationProfile.DYSLEXIA);

        mockMvc.perform(json(post("/api/lessons"), request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("title", "text")));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        CreateLessonRequest request = new CreateLessonRequest("   ", DemoLessons.WATER_CYCLE_TEXT,
                AdaptationProfile.DYSLEXIA);

        mockMvc.perform(json(post("/api/lessons"), request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Введите название урока")));
    }

    @Test
    void createRejectsUnknownProfile() throws Exception {
        String body = """
                {"title": "Круговорот воды", "text": "%s", "profile": "ADHD"}
                """.formatted(DemoLessons.WATER_CYCLE_TEXT);

        mockMvc.perform(post("/api/lessons").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("profile"))
                .andExpect(jsonPath("$.fieldErrors[0].message", containsString("DYSLEXIA, AUTISM")));
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/lessons").contentType(MediaType.APPLICATION_JSON).content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Тело запроса не является корректным JSON"));
    }

    @Test
    void updateContentReturnsFieldErrorsForInvalidContent() throws Exception {
        long id = createLesson(waterCycleRequest());
        AdaptedLesson valid = validLesson();
        AdaptedLesson invalid = new AdaptedLesson(
                valid.sentences(),
                List.of(),
                List.of(new Question("Вопрос?", List.of("Да", "Нет"), 5)));

        mockMvc.perform(json(put("/api/lessons/{id}/content", id), invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("cards", "quiz[0].correctIndexValid")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'quiz[0].correctIndexValid')].message")
                        .value("correctIndex должен указывать на существующий вариант ответа"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'cards')].message")
                        .value("не должно быть пустым"));

        mockMvc.perform(get("/api/lessons/{id}", id))
                .andExpect(jsonPath("$.content.cards", not(hasSize(0))));
    }

    @Test
    void unknownLessonReturnsNotFound() throws Exception {
        List<MockHttpServletRequestBuilder> requests = List.of(
                get("/api/lessons/{id}", UNKNOWN_ID),
                json(put("/api/lessons/{id}/content", UNKNOWN_ID), validLesson()),
                post("/api/lessons/{id}/approve", UNKNOWN_ID),
                post("/api/lessons/{id}/regenerate", UNKNOWN_ID),
                delete("/api/lessons/{id}", UNKNOWN_ID),
                get("/api/lessons/{id}/student", UNKNOWN_ID));

        for (MockHttpServletRequestBuilder request : requests) {
            mockMvc.perform(request)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Урок с id=" + UNKNOWN_ID + " не найден"))
                    .andExpect(jsonPath("$.timestamp").value(notNullValue()));
        }
    }

    @Test
    void nonNumericIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/lessons/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"));
    }

    @Test
    void unsupportedMethodUsesUnifiedErrorFormat() throws Exception {
        mockMvc.perform(put("/api/lessons"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message", endsWith("не поддерживается для данного адреса")));
    }

    private long createLesson(CreateLessonRequest request) throws Exception {
        String response = mockMvc.perform(json(post("/api/lessons"), request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode lesson = objectMapper.readTree(response);
        return lesson.get("id").asLong();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private static CreateLessonRequest waterCycleRequest() {
        return new CreateLessonRequest(
                DemoLessons.WATER_CYCLE_TITLE, DemoLessons.WATER_CYCLE_TEXT, AdaptationProfile.DYSLEXIA);
    }
}
