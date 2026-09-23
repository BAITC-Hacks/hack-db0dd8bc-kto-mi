package com.qadam.service;

import com.qadam.dto.AnalyzeTaskRequest;
import com.qadam.dto.CreateTaskRequest;
import com.qadam.dto.IndustryResponse;
import com.qadam.dto.Rating;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.dto.TaskResponse;
import com.qadam.model.Industry;
import com.qadam.model.RatingLevel;
import com.qadam.model.Task;
import com.qadam.model.TaskStatus;
import com.qadam.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Task workflow: AI analysis of a draft, card creation, manual editing with rating recalculation,
 * manual publication and the catalog. The LLM is always called outside DB transactions.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAiService taskAiService;
    private final RatingService ratingService;

    public TaskAnalysis analyze(AnalyzeTaskRequest request) {
        return taskAiService.analyze(request.draftText(), request.industry());
    }

    /** Builds the card with the AI and saves the task as {@code DRAFT}; nothing is saved if the AI fails. */
    public TaskResponse create(CreateTaskRequest request) {
        TaskCard card = taskAiService.buildCard(request.draftText(), request.industry(), request.answers());
        return toResponse(save(request.industry(), request.draftText(), card, TaskStatus.DRAFT));
    }

    /** Saves a task with a ready card and its rating; also used for demo data. */
    public Task save(Industry industry, String draftText, TaskCard card, TaskStatus status) {
        Task task = new Task();
        task.setIndustry(industry);
        task.setDraftText(draftText);
        task.setStatus(status);
        applyCard(task, card);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(long id) {
        return toResponse(find(id));
    }

    @Transactional
    public TaskResponse updateCard(long id, TaskCard card) {
        Task task = find(id);
        applyCard(task, card);
        return toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse publish(long id) {
        Task task = find(id);
        task.setStatus(TaskStatus.PUBLISHED);
        return toResponse(taskRepository.saveAndFlush(task));
    }

    /**
     * Published tasks, best rated first. Both filters are optional.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> catalog(Industry industry, RatingLevel level) {
        return taskRepository.findAllByStatusOrderByScoreDescIdAsc(TaskStatus.PUBLISHED).stream()
                .filter(task -> industry == null || task.getIndustry() == industry)
                .filter(task -> level == null || task.getLevel() == level)
                .map(this::toResponse)
                .toList();
    }

    public List<IndustryResponse> industries() {
        return Arrays.stream(Industry.values()).map(IndustryResponse::of).toList();
    }

    public TaskResponse toResponse(Task task) {
        Rating rating = ratingService.rate(task.getCard());
        boolean needsClarification = rating.level() == RatingLevel.DRAFT;
        return new TaskResponse(
                task.getId(),
                task.getIndustry(),
                task.getIndustry().getDisplayName(),
                task.getStatus(),
                task.getDraftText(),
                task.getCard(),
                rating,
                needsClarification,
                needsClarification ? TaskResponse.CLARIFICATION_NOTE : null,
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    Task find(long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(NotFoundException.Resource.TASK, id));
    }

    private void applyCard(Task task, TaskCard card) {
        task.setCard(card);
        Rating rating = ratingService.rate(task.getCard());
        task.setScore(rating.score());
        task.setLevel(rating.level());
    }
}
