package com.qadam.service;

import com.qadam.dto.TaskCard;
import com.qadam.model.Industry;
import com.qadam.model.Task;
import com.qadam.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMatcherTest {

    private final TaskMatcher matcher = new TaskMatcher();

    @Test
    void countsTeamKeywordsFoundInTaskText() {
        Task task = task(Industry.RETAIL, "Нужна аналитика продаж, данные в SQL-базе, на выходе дашборд в Power BI.");
        Team team = new Team("Команда", List.of("аналитика", "медицина"), List.of("анализ данных"),
                List.of("SQL", "Power BI", "Kotlin"));

        TaskMatcher.Match match = matcher.match(team, task);

        assertThat(match.matchedKeywords()).containsExactly("аналитика", "анализ данных", "SQL", "Power BI");
        assertThat(match.score()).isEqualTo(4);
    }

    @Test
    void industryNameCountsAsTaskText() {
        Team team = new Team("Команда", List.of("логистика"), List.of(), List.of());

        assertThat(matcher.match(team, task(Industry.LOGISTICS, "Оптимизировать доставку")).score()).isEqualTo(1);
        assertThat(matcher.match(team, task(Industry.RETAIL, "Оптимизировать доставку")).score()).isZero();
    }

    @Test
    void ignoresStopWordsAndCase() {
        assertThat(TaskMatcher.stems("Бот ДЛЯ сотрудников и на сайте")).containsExactly("бот", "сотру", "сайте");
    }

    private static Task task(Industry industry, String need) {
        Task task = new Task();
        task.setIndustry(industry);
        task.setCard(new TaskCard("Задача", "", need, "", "", "", "", "", "", ""));
        return task;
    }
}
