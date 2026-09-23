package com.qadam.service;

import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.CreateLessonRequest;
import com.qadam.dto.LessonResponse;
import com.qadam.dto.LessonSummaryResponse;
import com.qadam.dto.ProfileResponse;
import com.qadam.dto.StudentLessonResponse;
import com.qadam.model.AdaptationProfile;
import com.qadam.model.Lesson;
import com.qadam.model.LessonStatus;
import com.qadam.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Lesson workflow: a teacher creates a lesson, which is adapted right away and saved as a draft,
 * reviews and edits the adapted content and approves it; only approved lessons are shown to children.
 * Any change of the content returns the lesson to {@link LessonStatus#DRAFT}.
 * <p>
 * The LLM call can take up to a minute, so {@link #create} and {@link #regenerate} run it
 * outside of a database transaction; a failed adaptation leaves the database untouched.
 */
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonAdaptationService adaptationService;

    public LessonResponse create(CreateLessonRequest request) {
        String title = request.title().strip();
        String text = request.text().strip();
        AdaptedLesson content = adaptationService.adapt(title, text, request.profile());

        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setOriginalText(text);
        lesson.setProfile(request.profile());
        lesson.setStatus(LessonStatus.DRAFT);
        lesson.setAdaptedContent(content);
        return toResponse(lessonRepository.saveAndFlush(lesson));
    }

    @Transactional(readOnly = true)
    public List<LessonSummaryResponse> list() {
        return lessonRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(LessonService::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonResponse get(long id) {
        return toResponse(find(id));
    }

    @Transactional
    public LessonResponse updateContent(long id, AdaptedLesson content) {
        Lesson lesson = find(id);
        lesson.setAdaptedContent(content);
        lesson.setStatus(LessonStatus.DRAFT);
        return toResponse(lessonRepository.saveAndFlush(lesson));
    }

    @Transactional
    public LessonResponse approve(long id) {
        Lesson lesson = find(id);
        lesson.setStatus(LessonStatus.APPROVED);
        return toResponse(lessonRepository.saveAndFlush(lesson));
    }

    public LessonResponse regenerate(long id) {
        Lesson lesson = find(id);
        AdaptedLesson content = adaptationService.adapt(lesson.getTitle(), lesson.getOriginalText(), lesson.getProfile());
        lesson.setAdaptedContent(content);
        lesson.setStatus(LessonStatus.DRAFT);
        return toResponse(lessonRepository.saveAndFlush(lesson));
    }

    @Transactional
    public void delete(long id) {
        lessonRepository.delete(find(id));
    }

    @Transactional(readOnly = true)
    public StudentLessonResponse getStudentView(long id) {
        Lesson lesson = find(id);
        if (lesson.getStatus() != LessonStatus.APPROVED) {
            throw new LessonNotApprovedException(id);
        }
        return new StudentLessonResponse(
                lesson.getTitle(),
                lesson.getProfile().getDisplaySettings(),
                lesson.getAdaptedContent());
    }

    public List<ProfileResponse> listProfiles() {
        return Arrays.stream(AdaptationProfile.values())
                .map(ProfileResponse::of)
                .toList();
    }

    private Lesson find(long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new LessonNotFoundException(id));
    }

    private static LessonResponse toResponse(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getOriginalText(),
                lesson.getProfile(),
                lesson.getProfile().getDisplayName(),
                lesson.getStatus(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt(),
                lesson.getAdaptedContent());
    }

    private static LessonSummaryResponse toSummary(Lesson lesson) {
        return new LessonSummaryResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getProfile(),
                lesson.getStatus(),
                lesson.getCreatedAt());
    }
}
