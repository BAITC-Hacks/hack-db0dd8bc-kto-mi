package com.qadam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qadam.dto.TaskCard;
import com.qadam.model.Industry;
import com.qadam.model.Task;
import com.qadam.model.TaskStatus;
import com.qadam.model.Team;
import com.qadam.repository.ProposalRepository;
import com.qadam.repository.TaskRepository;
import com.qadam.repository.TeamRepository;
import com.qadam.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task, catalog, team and proposal API over the mock LLM (see test {@code application.properties}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskFlowTest {

    private static final String DRAFT = "Мы сеть из 12 кофеен в Алматы. "
            + "Хотим понять, почему в будни после обеда падают продажи. "
            + "Есть выгрузка чеков из кассовой системы за 2 года в Excel.";

    private static final String TEXT = "Достаточно длинное описание поля";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private TaskService taskService;

    @BeforeEach
    void cleanDatabase() {
        proposalRepository.deleteAll();
        taskRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    void fullScenarioFromDraftToAcceptedProposal() throws Exception {
        // 1. Analyze the draft: questions only about missing fields
        mockMvc.perform(json(post("/api/tasks/analyze"), Map.of("draftText", DRAFT, "industry", "HORECA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missingFields", hasItem("users")))
                .andExpect(jsonPath("$.missingFields", hasItem("contact")))
                .andExpect(jsonPath("$.questions", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.questions[0].field").value("users"))
                .andExpect(jsonPath("$.questions[0].question").isNotEmpty());

        // 2. Create the task with an answer: DRAFT status, card built from the draft only
        String created = mockMvc.perform(json(post("/api/tasks"), Map.of(
                        "draftText", DRAFT,
                        "industry", "HORECA",
                        "answers", List.of(Map.of("field", "contact", "answer", "ops@example.com")))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/tasks/")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.industry").value("HORECA"))
                .andExpect(jsonPath("$.industryName").value("Гостиницы и общепит"))
                .andExpect(jsonPath("$.draftText").value(DRAFT))
                .andExpect(jsonPath("$.context").value("Мы сеть из 12 кофеен в Алматы."))
                .andExpect(jsonPath("$.contact").value("ops@example.com"))
                .andExpect(jsonPath("$.users").value(""))
                .andExpect(jsonPath("$.rating.score").value(45))
                .andExpect(jsonPath("$.rating.level").value("WORKING"))
                .andExpect(jsonPath("$.rating.levelName").value("Рабочая задача"))
                .andExpect(jsonPath("$.rating.breakdown", hasSize(7)))
                .andExpect(jsonPath("$.rating.missing", hasItem("users")))
                .andExpect(jsonPath("$.rating.tips", hasItem("Заполните «Ожидаемый результат»: +15 баллов")))
                .andExpect(jsonPath("$.needsClarification").value(false))
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(created).get("id").asLong();

        // 3. Edit the card: the rating grows, the status stays DRAFT
        mockMvc.perform(json(put("/api/tasks/{id}/card", taskId), fullCard()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Продажи кофеен после обеда"))
                .andExpect(jsonPath("$.rating.score").value(100))
                .andExpect(jsonPath("$.rating.level").value("PRIORITY"))
                .andExpect(jsonPath("$.rating.missing", hasSize(0)))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating.score").value(100));

        // 4. Not in the catalog before publication
        mockMvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 5. Manual publication
        mockMvc.perform(post("/api/tasks/{id}/publish", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // 6. Catalog with filters
        mockMvc.perform(get("/api/catalog"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taskId))
                .andExpect(jsonPath("$[0].rating.score").value(100));
        mockMvc.perform(get("/api/catalog").param("industry", "HORECA").param("level", "PRIORITY"))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/catalog").param("industry", "RETAIL"))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/catalog").param("industry", "").param("level", "DRAFT"))
                .andExpect(jsonPath("$", hasSize(0)));

        // 7. A team sees the task in its recommendations and sends a proposal
        Team team = teamRepository.save(new Team("Кофейные аналитики", List.of("общепит"),
                List.of("анализ продаж"), List.of("Python", "Excel")));
        mockMvc.perform(get("/api/teams/{id}/recommendations", team.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].task.id").value(taskId))
                .andExpect(jsonPath("$[0].matchScore").value(3))
                .andExpect(jsonPath("$[0].matchedKeywords", hasItem("Excel")));

        String proposal = mockMvc.perform(json(post("/api/tasks/{id}/proposals", taskId), proposalBody(team.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.teamName").value("Кофейные аналитики"))
                .andExpect(jsonPath("$.teamPoints").value(0))
                .andReturn().getResponse().getContentAsString();
        long proposalId = objectMapper.readTree(proposal).get("id").asLong();
        mockMvc.perform(json(post("/api/tasks/{id}/proposals", taskId), proposalBody(team.getId())))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/tasks/{id}/proposals", taskId))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(proposalId));

        // 8. The business accepts the proposal manually and confirms a milestone
        mockMvc.perform(post("/api/proposals/{id}/accept", proposalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(post("/api/proposals/{id}/confirm-milestone", proposalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedMilestones").value(1))
                .andExpect(jsonPath("$.teamPoints").value(10));
        mockMvc.perform(get("/api/teams"))
                .andExpect(jsonPath("$[0].points").value(10));
    }

    @Test
    void cannotProposeForUnpublishedTask() throws Exception {
        Task task = taskService.save(Industry.IT, DRAFT, fullCard(), TaskStatus.DRAFT);
        Team team = teamRepository.save(new Team("Команда", List.of(), List.of(), List.of()));

        mockMvc.perform(json(post("/api/tasks/{id}/proposals", task.getId()), proposalBody(team.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Откликнуться можно только на опубликованную задачу"));

        assertThat(proposalRepository.count()).isZero();
    }

    @Test
    void catalogIsSortedByScoreAndMarksDraftLevelTasks() throws Exception {
        TaskCard weak = new TaskCard("Слабая", TEXT, "", "", "", "", "", "", "", "");
        TaskCard medium = new TaskCard("Средняя", TEXT, TEXT, "", TEXT, "", "", "", "", "");
        Task weakTask = taskService.save(Industry.IT, DRAFT, weak, TaskStatus.PUBLISHED);
        Task strongTask = taskService.save(Industry.IT, DRAFT, fullCard(), TaskStatus.PUBLISHED);
        Task mediumTask = taskService.save(Industry.RETAIL, DRAFT, medium, TaskStatus.PUBLISHED);
        taskService.save(Industry.IT, DRAFT, fullCard(), TaskStatus.DRAFT);

        mockMvc.perform(get("/api/catalog"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(strongTask.getId()))
                .andExpect(jsonPath("$[1].id").value(mediumTask.getId()))
                .andExpect(jsonPath("$[2].id").value(weakTask.getId()))
                .andExpect(jsonPath("$[0].needsClarification").value(false))
                .andExpect(jsonPath("$[0].clarificationNote").value(nullValue()))
                .andExpect(jsonPath("$[2].rating.level").value("DRAFT"))
                .andExpect(jsonPath("$[2].needsClarification").value(true))
                .andExpect(jsonPath("$[2].clarificationNote").value("требует уточнения"));
        mockMvc.perform(get("/api/catalog").param("level", "WORKING"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(mediumTask.getId()));
    }

    @Test
    void recommendationsSkipDraftLevelAndUnpublishedTasksAndSortByMatch() throws Exception {
        TaskCard logistics = new TaskCard("Маршруты", "Курьерская служба доставки", TEXT, "", TEXT, "", "", "", "", "");
        Task generic = taskService.save(Industry.IT, DRAFT, fullCard(), TaskStatus.PUBLISHED);
        Task matching = taskService.save(Industry.LOGISTICS, DRAFT, logistics, TaskStatus.PUBLISHED);
        taskService.save(Industry.LOGISTICS, DRAFT, new TaskCard("Слабая", "Курьерская служба доставки",
                "", "", "", "", "", "", "", ""), TaskStatus.PUBLISHED);
        taskService.save(Industry.LOGISTICS, DRAFT, logistics, TaskStatus.DRAFT);
        Team team = teamRepository.save(new Team("Маршрут", List.of("логистика", "доставка"), List.of(), List.of()));

        mockMvc.perform(get("/api/teams/{id}/recommendations", team.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].task.id").value(matching.getId()))
                .andExpect(jsonPath("$[0].matchScore").value(2))
                .andExpect(jsonPath("$[1].task.id").value(generic.getId()))
                .andExpect(jsonPath("$[1].matchScore").value(0));
    }

    @Test
    void acceptRejectAndMilestoneRules() throws Exception {
        Task task = taskService.save(Industry.IT, DRAFT, fullCard(), TaskStatus.PUBLISHED);
        Team team = teamRepository.save(new Team("Команда", List.of(), List.of(), List.of()));
        long first = proposalId(task, team);
        long second = proposalId(task, team);
        long third = proposalId(task, team);

        mockMvc.perform(post("/api/proposals/{id}/accept", first)).andExpect(status().isOk());
        mockMvc.perform(post("/api/proposals/{id}/accept", second)).andExpect(status().isOk());
        mockMvc.perform(post("/api/proposals/{id}/reject", third))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/proposals/{id}/accept", third))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Отклик уже рассмотрен"));
        mockMvc.perform(post("/api/proposals/{id}/confirm-milestone", third))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Подтвердить этап можно только для принятого отклика"));
        mockMvc.perform(post("/api/proposals/{id}/confirm-milestone", first)).andExpect(status().isOk());
        mockMvc.perform(post("/api/proposals/{id}/confirm-milestone", second))
                .andExpect(jsonPath("$.teamPoints").value(20));
    }

    @Test
    void validationErrorsOnAnalyzeAndCreate() throws Exception {
        mockMvc.perform(json(post("/api/tasks/analyze"), Map.of("draftText", "Коротко")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Проверьте правильность заполнения полей"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("draftText")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("industry")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Выберите отрасль")));

        mockMvc.perform(json(post("/api/tasks"), Map.of("draftText", DRAFT, "industry", "SPACE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("industry"))
                .andExpect(jsonPath("$.fieldErrors[0].message", containsString("RETAIL")));

        mockMvc.perform(json(post("/api/tasks"), Map.of("draftText", DRAFT, "industry", "IT",
                        "answers", List.of(Map.of("field", "budget", "answer", "100")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("answers[0].field"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Неизвестное поле карточки"));

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void validationErrorsOnCardAndProposal() throws Exception {
        Task task = taskService.save(Industry.IT, DRAFT, fullCard(), TaskStatus.PUBLISHED);

        mockMvc.perform(json(put("/api/tasks/{id}/card", task.getId()),
                        Map.of("title", "x".repeat(TaskCard.TITLE_MAX + 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));

        mockMvc.perform(json(post("/api/tasks/{id}/proposals", task.getId()),
                        Map.of("idea", "Идея", "plan", "", "duration", "", "prototypeUrl", "ftp://example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("teamId")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("idea")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("plan")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("duration")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("prototypeUrl")));

        mockMvc.perform(get("/api/catalog").param("level", "TOP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("level"));
    }

    @Test
    void notFoundErrorsAreInRussian() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Задача с id=999999 не найдена"));
        mockMvc.perform(get("/api/teams/{id}/recommendations", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Команда с id=999999 не найдена"));
        mockMvc.perform(post("/api/proposals/{id}/accept", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Отклик с id=999999 не найден"));

        Task task = taskService.save(Industry.IT, DRAFT, fullCard(), TaskStatus.PUBLISHED);
        mockMvc.perform(json(post("/api/tasks/{id}/proposals", task.getId()), proposalBody(999_999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Команда с id=999999 не найдена"));
    }

    @Test
    void industriesAreListedWithRussianNames() throws Exception {
        mockMvc.perform(get("/api/industries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(Industry.values().length)))
                .andExpect(jsonPath("$[0].code").value("IT"))
                .andExpect(jsonPath("$[1].name").value("Розничная торговля"));
    }

    private long proposalId(Task task, Team team) throws Exception {
        String body = mockMvc.perform(json(post("/api/tasks/{id}/proposals", task.getId()), proposalBody(team.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private static Map<String, Object> proposalBody(long teamId) {
        return Map.of(
                "teamId", teamId,
                "idea", "Построим дашборд продаж по часам",
                "plan", "Анализ данных, дашборд, рекомендации",
                "duration", "5 недель",
                "prototypeUrl", "https://example.com/prototype");
    }

    private static TaskCard fullCard() {
        return new TaskCard(
                "Продажи кофеен после обеда",
                "Мы сеть из 12 кофеен в Алматы.",
                "Хотим понять, почему в будни после обеда падают продажи.",
                "Управляющие кофейнями и маркетолог сети.",
                "Выгрузка чеков из кассовой системы за 2 года в Excel.",
                "Персональные данные гостей не передаются, срок — 6 недель.",
                "Дашборд с продажами по часам и рекомендации по акциям.",
                "Рост дневной выручки после обеда на 10% за 2 месяца.",
                "Операционный менеджер, ops@example.com",
                "Онлайн-встреча раз в неделю и общий чат.");
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }
}
