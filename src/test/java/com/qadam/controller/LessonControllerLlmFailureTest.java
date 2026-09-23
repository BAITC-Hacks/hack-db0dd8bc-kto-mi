package com.qadam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.dto.CreateLessonRequest;
import com.qadam.llm.LlmClient;
import com.qadam.llm.LlmException;
import com.qadam.model.AdaptationProfile;
import com.qadam.model.Lesson;
import com.qadam.model.LessonStatus;
import com.qadam.repository.LessonRepository;
import com.qadam.service.DemoLessons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.qadam.dto.AdaptedLessonFixtures.validLesson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LessonControllerLlmFailureTest {

    private static final String LLM_FAILURE_MESSAGE =
            "Не удалось адаптировать текст урока. Попробуйте ещё раз чуть позже";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LessonRepository lessonRepository;

    @MockitoBean
    private LlmClient llmClient;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        when(llmClient.adapt(anyString(), anyString(), any()))
                .thenThrow(new LlmException("OpenAI API returned HTTP 503"));
    }

    @Test
    void createReturnsBadGatewayAndSavesNothing() throws Exception {
        CreateLessonRequest request = new CreateLessonRequest(
                DemoLessons.WATER_CYCLE_TITLE, DemoLessons.WATER_CYCLE_TEXT, AdaptationProfile.DYSLEXIA);

        mockMvc.perform(post("/api/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value(LLM_FAILURE_MESSAGE))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(lessonRepository.count()).isZero();
    }

    @Test
    void regenerateReturnsBadGatewayAndKeepsLesson() throws Exception {
        Lesson lesson = new Lesson();
        lesson.setTitle(DemoLessons.WATER_CYCLE_TITLE);
        lesson.setOriginalText(DemoLessons.WATER_CYCLE_TEXT);
        lesson.setProfile(AdaptationProfile.DYSLEXIA);
        lesson.setStatus(LessonStatus.APPROVED);
        lesson.setAdaptedContent(validLesson());
        long id = lessonRepository.save(lesson).getId();

        mockMvc.perform(post("/api/lessons/{id}/regenerate", id))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(LLM_FAILURE_MESSAGE));

        Lesson unchanged = lessonRepository.findById(id).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(LessonStatus.APPROVED);
        assertThat(unchanged.getAdaptedContent()).isEqualTo(validLesson());
    }
}
