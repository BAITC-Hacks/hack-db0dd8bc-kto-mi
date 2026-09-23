package com.qadam.controller;

import com.qadam.dto.ProfileResponse;
import com.qadam.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Профили", description = "Профили адаптации и настройки отображения урока для ребёнка")
@RestController
@RequestMapping(value = "/api/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProfileController {

    private final LessonService lessonService;

    @Operation(
            summary = "Список профилей адаптации",
            description = "Код профиля (передаётся как profile при создании урока), название на русском "
                    + "и настройки отображения: шрифт, межстрочный интервал, фон, пиктограммы, структура.")
    @ApiResponse(responseCode = "200", description = "Список профилей")
    @GetMapping
    public List<ProfileResponse> list() {
        return lessonService.listProfiles();
    }
}
