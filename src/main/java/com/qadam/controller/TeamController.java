package com.qadam.controller;

import com.qadam.dto.ErrorResponse;
import com.qadam.dto.RecommendationResponse;
import com.qadam.dto.TeamResponse;
import com.qadam.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Команды", description = "Студенческие команды и рекомендации задач для них")
@RestController
@RequestMapping(value = "/api/teams", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "Список команд", description = "Интересы, навыки, технологии и баллы команд.")
    @ApiResponse(responseCode = "200", description = "Команды")
    @GetMapping
    public List<TeamResponse> list() {
        return teamService.list();
    }

    @Operation(summary = "Рекомендации для команды",
            description = "Опубликованные задачи уровня от WORKING, отсортированные по числу совпадений "
                    + "интересов, навыков и технологий команды со словами задачи (без ИИ).")
    @ApiResponse(responseCode = "200", description = "Рекомендованные задачи")
    @ApiResponse(responseCode = "404", description = "Команда не найдена",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}/recommendations")
    public List<RecommendationResponse> recommendations(@PathVariable long id) {
        return teamService.recommendations(id);
    }
}
