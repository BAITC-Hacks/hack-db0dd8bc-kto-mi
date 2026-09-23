package com.qadam.controller;

import com.qadam.dto.CreateProposalRequest;
import com.qadam.dto.ErrorResponse;
import com.qadam.dto.ProposalResponse;
import com.qadam.service.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Отклики", description = "Отклики команд на опубликованные задачи; решения принимает только бизнес")
@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@ApiResponse(responseCode = "404", description = "Задача, команда или отклик не найдены",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class ProposalController {

    private final ProposalService proposalService;

    @Operation(
            summary = "Откликнуться на задачу",
            description = "Только для опубликованных задач. Число откликов не ограничено.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    schema = @Schema(implementation = CreateProposalRequest.class),
                    examples = @ExampleObject(value = ApiExamples.CREATE_PROPOSAL))))
    @ApiResponse(responseCode = "201", description = "Отклик создан (PENDING)")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Задача не опубликована", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.NOT_PUBLISHED)))
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/tasks/{taskId}/proposals", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProposalResponse create(@PathVariable long taskId, @Valid @RequestBody CreateProposalRequest request) {
        return proposalService.create(taskId, request);
    }

    @Operation(summary = "Отклики на задачу", description = "В порядке поступления.")
    @ApiResponse(responseCode = "200", description = "Отклики")
    @GetMapping("/tasks/{taskId}/proposals")
    public List<ProposalResponse> list(@PathVariable long taskId) {
        return proposalService.listForTask(taskId);
    }

    @Operation(summary = "Принять отклик",
            description = "Ручное действие бизнеса. Можно принять несколько откликов на задачу или ни одного.")
    @ApiResponse(responseCode = "200", description = "Отклик принят (ACCEPTED)")
    @ApiResponse(responseCode = "409", description = "Отклик уже рассмотрен",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/proposals/{id}/accept")
    public ProposalResponse accept(@PathVariable long id) {
        return proposalService.accept(id);
    }

    @Operation(summary = "Отклонить отклик", description = "Ручное действие бизнеса.")
    @ApiResponse(responseCode = "200", description = "Отклик отклонён (REJECTED)")
    @ApiResponse(responseCode = "409", description = "Отклик уже рассмотрен",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/proposals/{id}/reject")
    public ProposalResponse reject(@PathVariable long id) {
        return proposalService.reject(id);
    }

    @Operation(summary = "Подтвердить этап",
            description = "Бизнес подтверждает выполненный этап принятого отклика: команда получает +10 баллов.")
    @ApiResponse(responseCode = "200", description = "Этап подтверждён")
    @ApiResponse(responseCode = "409", description = "Отклик не принят",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/proposals/{id}/confirm-milestone")
    public ProposalResponse confirmMilestone(@PathVariable long id) {
        return proposalService.confirmMilestone(id);
    }
}
