package com.qadam.controller;

import com.qadam.dto.AnalyzeTaskRequest;
import com.qadam.dto.CreateTaskRequest;
import com.qadam.dto.ErrorResponse;
import com.qadam.dto.IndustryResponse;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.dto.TaskResponse;
import com.qadam.model.Industry;
import com.qadam.model.RatingLevel;
import com.qadam.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Задачи", description = "Черновик → вопросы ИИ → карточка с рейтингом → правка → публикация → каталог")
@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(
        schema = @Schema(implementation = ErrorResponse.class),
        examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR)))
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "Проанализировать черновик",
            description = "ИИ находит незаполненные поля карточки и задаёт не меньше 3 уточняющих вопросов. "
                    + "Ничего не сохраняет.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    schema = @Schema(implementation = AnalyzeTaskRequest.class),
                    examples = @ExampleObject(value = ApiExamples.ANALYZE))))
    @ApiResponse(responseCode = "200", description = "Недостающие поля и вопросы")
    @ApiResponse(responseCode = "502", description = "ИИ не смог обработать черновик", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.LLM_FAILURE)))
    @PostMapping(value = "/tasks/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskAnalysis analyze(@Valid @RequestBody AnalyzeTaskRequest request) {
        return taskService.analyze(request);
    }

    @Operation(
            summary = "Создать задачу",
            description = "ИИ собирает карточку строго из черновика и ответов (без выдуманных фактов, "
                    + "неизвестные поля пустые). Задача сохраняется в статусе DRAFT с рейтингом.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    schema = @Schema(implementation = CreateTaskRequest.class),
                    examples = @ExampleObject(value = ApiExamples.CREATE))))
    @ApiResponse(responseCode = "201", description = "Задача создана")
    @ApiResponse(responseCode = "502", description = "ИИ не смог собрать карточку, задача не сохранена",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = ApiExamples.LLM_FAILURE)))
    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = taskService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(task.id())
                .toUri();
        return ResponseEntity.created(location).body(task);
    }

    @Operation(summary = "Задача целиком", description = "Черновик, поля карточки, статус и рейтинг.")
    @ApiResponse(responseCode = "200", description = "Задача найдена")
    @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @GetMapping("/tasks/{id}")
    public TaskResponse get(@PathVariable long id) {
        return taskService.get(id);
    }

    @Operation(
            summary = "Изменить карточку",
            description = "Заменяет все поля карточки (отсутствующее поле становится пустым) и пересчитывает рейтинг. "
                    + "Статус не меняется.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    schema = @Schema(implementation = TaskCard.class),
                    examples = @ExampleObject(value = ApiExamples.UPDATE_CARD))))
    @ApiResponse(responseCode = "200", description = "Карточка сохранена, рейтинг пересчитан")
    @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @PutMapping(value = "/tasks/{id}/card", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TaskResponse updateCard(@PathVariable long id, @Valid @RequestBody TaskCard card) {
        return taskService.updateCard(id, card);
    }

    @Operation(summary = "Опубликовать задачу",
            description = "Ручное подтверждение бизнесом: статус PUBLISHED, задача появляется в каталоге.")
    @ApiResponse(responseCode = "200", description = "Задача опубликована")
    @ApiResponse(responseCode = "404", description = "Задача не найдена", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @PostMapping("/tasks/{id}/publish")
    public TaskResponse publish(@PathVariable long id) {
        return taskService.publish(id);
    }

    @Operation(summary = "Каталог задач",
            description = "Только опубликованные задачи, по убыванию рейтинга. Задачи уровня DRAFT видны, "
                    + "но с пометкой «требует уточнения» (needsClarification = true).")
    @ApiResponse(responseCode = "200", description = "Опубликованные задачи")
    @GetMapping("/catalog")
    public List<TaskResponse> catalog(
            @Parameter(description = "Код отрасли, например RETAIL") @RequestParam(required = false) Industry industry,
            @Parameter(description = "Уровень: DRAFT, WORKING, READY, PRIORITY") @RequestParam(required = false)
            RatingLevel level) {
        return taskService.catalog(industry, level);
    }

    @Operation(summary = "Отрасли", description = "Коды и названия отраслей для поля industry.")
    @ApiResponse(responseCode = "200", description = "Список отраслей")
    @GetMapping("/industries")
    public List<IndustryResponse> industries() {
        return taskService.industries();
    }
}
