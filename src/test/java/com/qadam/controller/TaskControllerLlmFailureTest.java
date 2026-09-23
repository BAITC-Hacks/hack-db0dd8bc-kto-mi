package com.qadam.controller;

import com.qadam.llm.LlmClient;
import com.qadam.llm.LlmException;
import com.qadam.llm.LlmInvalidResponseException;
import com.qadam.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerLlmFailureTest {

    private static final String BODY = """
            {"draftText": "Мы сеть кофеен. Хотим понять, почему падают продажи.", "industry": "HORECA"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @MockitoBean
    private LlmClient llmClient;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
    }

    @Test
    void unavailableLlmReturns502AndSavesNothing() throws Exception {
        when(llmClient.buildCard(any(), any(), any())).thenThrow(new LlmException("HTTP 503"));

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message")
                        .value("Не удалось обработать задачу с помощью ИИ. Попробуйте ещё раз чуть позже"));

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void invalidAnalysisReturns502() throws Exception {
        when(llmClient.analyze(any(), any())).thenThrow(new LlmInvalidResponseException("not JSON"));

        mockMvc.perform(post("/api/tasks/analyze").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway());
    }
}
