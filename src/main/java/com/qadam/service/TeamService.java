package com.qadam.service;

import com.qadam.dto.RecommendationResponse;
import com.qadam.dto.TeamResponse;
import com.qadam.model.RatingLevel;
import com.qadam.model.TaskStatus;
import com.qadam.model.Team;
import com.qadam.repository.TaskRepository;
import com.qadam.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Student teams and task recommendations for them.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    /** Only tasks of this level or higher are recommended. */
    static final RatingLevel MIN_RECOMMENDED_LEVEL = RatingLevel.WORKING;

    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final TaskMatcher taskMatcher;

    @Transactional(readOnly = true)
    public List<TeamResponse> list() {
        return teamRepository.findAll().stream().map(TeamResponse::of).toList();
    }

    /**
     * Published tasks of level {@code WORKING} or higher, sorted by the number of matching team keywords,
     * then by rating score.
     */
    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommendations(long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.TEAM, teamId));
        return taskRepository.findAllByStatusOrderByScoreDescIdAsc(TaskStatus.PUBLISHED).stream()
                .filter(task -> task.getLevel().compareTo(MIN_RECOMMENDED_LEVEL) >= 0)
                .map(task -> {
                    TaskMatcher.Match match = taskMatcher.match(team, task);
                    return new RecommendationResponse(taskService.toResponse(task), match.score(),
                            match.matchedKeywords());
                })
                .sorted(Comparator.comparingInt(RecommendationResponse::matchScore).reversed())
                .toList();
    }
}
