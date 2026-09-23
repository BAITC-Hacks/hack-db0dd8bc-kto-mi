package com.qadam.controller;

import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.CreateLessonRequest;
import com.qadam.dto.ErrorResponse;
import com.qadam.dto.LessonResponse;
import com.qadam.dto.LessonSummaryResponse;
import com.qadam.dto.StudentLessonResponse;
import com.qadam.service.LessonService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Уроки", description = "Создание, адаптация, проверка и утверждение уроков учителем; просмотр урока ребёнком")
@RestController
@RequestMapping(value = "/api/lessons", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @Operation(
            summary = "Создать урок и адаптировать его",
            description = "Сохраняет текст урока, адаптирует его под выбранный профиль (LLM + пиктограммы ARASAAC) "
                    + "и возвращает урок в статусе DRAFT. Адаптация может занять до минуты.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    schema = @Schema(implementation = CreateLessonRequest.class),
                    examples = {
                            @ExampleObject(name = "Круговорот воды (дислексия)", value = ApiExamples.CREATE_WATER_CYCLE),
                            @ExampleObject(name = "Части растения (аутизм)", value = ApiExamples.CREATE_PLANT_PARTS)
                    })))
    @ApiResponse(responseCode = "201", description = "Урок создан и адаптирован")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR)))
    @ApiResponse(responseCode = "502", description = "LLM не смог адаптировать текст, урок не сохранён", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.LLM_FAILURE)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LessonResponse> create(@Valid @RequestBody CreateLessonRequest request) {
        LessonResponse lesson = lessonService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(lesson.id())
                .toUri();
        return ResponseEntity.created(location).body(lesson);
    }

    @Operation(summary = "Список уроков", description = "Краткая информация обо всех уроках, новые сверху.")
    @ApiResponse(responseCode = "200", description = "Список уроков")
    @GetMapping
    public List<LessonSummaryResponse> list() {
        return lessonService.list();
    }

    @Operation(summary = "Урок целиком", description = "Исходный текст, адаптированный контент и статус урока.")
    @ApiResponse(responseCode = "200", description = "Урок найден")
    @ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @GetMapping("/{id}")
    public LessonResponse get(@Parameter(description = "Id урока", example = "1") @PathVariable long id) {
        return lessonService.get(id);
    }

    @Operation(
            summary = "Отредактировать адаптированный контент",
            description = "Учитель заменяет адаптированный контент целиком (предложения, карточки, тест). "
                    + "После правки урок снова в статусе DRAFT и должен быть утверждён повторно.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    schema = @Schema(implementation = AdaptedLesson.class),
                    examples = @ExampleObject(name = "Сокращённый круговорот воды", value = ApiExamples.UPDATE_CONTENT))))
    @ApiResponse(responseCode = "200", description = "Контент сохранён, статус DRAFT")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR)))
    @ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @PutMapping(value = "/{id}/content", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LessonResponse updateContent(@Parameter(description = "Id урока", example = "1") @PathVariable long id,
                                        @Valid @RequestBody AdaptedLesson content) {
        return lessonService.updateContent(id, content);
    }

    @Operation(summary = "Утвердить урок", description = "Переводит урок в статус APPROVED: теперь его видит ребёнок.")
    @ApiResponse(responseCode = "200", description = "Урок утверждён")
    @ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @PostMapping("/{id}/approve")
    public LessonResponse approve(@Parameter(description = "Id урока", example = "1") @PathVariable long id) {
        return lessonService.approve(id);
    }

    @Operation(
            summary = "Адаптировать урок заново",
            description = "Повторно адаптирует исходный текст урока. Правки учителя заменяются новым результатом, "
                    + "статус снова DRAFT. При ошибке LLM урок не меняется.")
    @ApiResponse(responseCode = "200", description = "Урок адаптирован заново")
    @ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @ApiResponse(responseCode = "502", description = "LLM не смог адаптировать текст, урок не изменён", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.LLM_FAILURE)))
    @PostMapping("/{id}/regenerate")
    public LessonResponse regenerate(@Parameter(description = "Id урока", example = "1") @PathVariable long id) {
        return lessonService.regenerate(id);
    }

    @Operation(summary = "Удалить урок")
    @ApiResponse(responseCode = "204", description = "Урок удалён")
    @ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Id урока", example = "1") @PathVariable long id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Урок для ребёнка",
            description = "Только адаптированный контент, название и настройки отображения профиля. "
                    + "Доступен только для утверждённых уроков.")
    @ApiResponse(responseCode = "200", description = "Утверждённый урок")
    @ApiResponse(responseCode = "403", description = "Урок ещё не утверждён", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_APPROVED)))
    @ApiResponse(responseCode = "404", description = "Урок не найден", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    @GetMapping("/{id}/student")
    public StudentLessonResponse studentView(@Parameter(description = "Id урока", example = "1") @PathVariable long id) {
        return lessonService.getStudentView(id);
    }
}
