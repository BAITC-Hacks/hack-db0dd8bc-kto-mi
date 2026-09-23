package com.qadam.service;

import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.CreateLessonRequest;
import com.qadam.dto.LessonResponse;
import com.qadam.dto.ProfileResponse;
import com.qadam.dto.StudentLessonResponse;
import com.qadam.model.AdaptationProfile;
import com.qadam.model.Lesson;
import com.qadam.model.LessonStatus;
import com.qadam.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.qadam.dto.AdaptedLessonFixtures.validLesson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonServiceTest {

    private static final String TEXT = "Растения растут из семян. Семенам нужны вода, свет и тепло, чтобы прорасти.";

    private LessonRepository repository;
    private LessonAdaptationService adaptationService;
    private LessonService service;

    @BeforeEach
    void setUp() {
        repository = mock(LessonRepository.class);
        adaptationService = mock(LessonAdaptationService.class);
        service = new LessonService(repository, adaptationService);
        when(repository.saveAndFlush(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson lesson = invocation.getArgument(0);
            if (lesson.getId() == null) {
                lesson.setId(1L);
            }
            return lesson;
        });
    }

    @Test
    void createAdaptsAndSavesDraft() {
        when(adaptationService.adapt("Растения", TEXT, AdaptationProfile.AUTISM)).thenReturn(validLesson());

        LessonResponse lesson = service.create(new CreateLessonRequest(" Растения ", TEXT, AdaptationProfile.AUTISM));

        assertThat(lesson.id()).isEqualTo(1L);
        assertThat(lesson.title()).isEqualTo("Растения");
        assertThat(lesson.status()).isEqualTo(LessonStatus.DRAFT);
        assertThat(lesson.profileName()).isEqualTo("Аутизм (РАС)");
        assertThat(lesson.content()).isEqualTo(validLesson());
    }

    @Test
    void createSavesNothingWhenAdaptationFails() {
        when(adaptationService.adapt(any(), any(), any()))
                .thenThrow(new LlmAdaptationException("LLM is unavailable", null));

        assertThatThrownBy(() -> service.create(new CreateLessonRequest("Растения", TEXT, AdaptationProfile.AUTISM)))
                .isInstanceOf(LlmAdaptationException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateContentReturnsLessonToDraft() {
        Lesson lesson = existing(LessonStatus.APPROVED);
        AdaptedLesson edited = new AdaptedLesson(validLesson().sentences(), validLesson().cards(), List.of());

        LessonResponse response = service.updateContent(lesson.getId(), edited);

        assertThat(response.status()).isEqualTo(LessonStatus.DRAFT);
        assertThat(response.content()).isEqualTo(edited);
    }

    @Test
    void approveSetsApproved() {
        Lesson lesson = existing(LessonStatus.DRAFT);

        assertThat(service.approve(lesson.getId()).status()).isEqualTo(LessonStatus.APPROVED);
    }

    @Test
    void regenerateReplacesContentAndReturnsToDraft() {
        Lesson lesson = existing(LessonStatus.APPROVED);
        lesson.setAdaptedContent(null);
        when(adaptationService.adapt("Растения", TEXT, AdaptationProfile.DYSLEXIA)).thenReturn(validLesson());

        LessonResponse response = service.regenerate(lesson.getId());

        assertThat(response.status()).isEqualTo(LessonStatus.DRAFT);
        assertThat(response.content()).isEqualTo(validLesson());
    }

    @Test
    void regenerateKeepsLessonWhenAdaptationFails() {
        Lesson lesson = existing(LessonStatus.APPROVED);
        when(adaptationService.adapt(any(), any(), any()))
                .thenThrow(new LlmAdaptationException("LLM is unavailable", null));

        assertThatThrownBy(() -> service.regenerate(lesson.getId())).isInstanceOf(LlmAdaptationException.class);
        assertThat(lesson.getStatus()).isEqualTo(LessonStatus.APPROVED);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void studentViewOfApprovedLessonHasOnlyContentAndDisplaySettings() {
        Lesson lesson = existing(LessonStatus.APPROVED);

        StudentLessonResponse view = service.getStudentView(lesson.getId());

        assertThat(view.title()).isEqualTo("Растения");
        assertThat(view.displaySettings()).isEqualTo(AdaptationProfile.DYSLEXIA.getDisplaySettings());
        assertThat(view.content()).isEqualTo(validLesson());
    }

    @Test
    void studentViewOfDraftIsRejected() {
        Lesson lesson = existing(LessonStatus.DRAFT);

        assertThatThrownBy(() -> service.getStudentView(lesson.getId()))
                .isInstanceOf(LessonNotApprovedException.class);
    }

    @Test
    void unknownLessonIsNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(42L))
                .isInstanceOf(LessonNotFoundException.class)
                .hasFieldOrPropertyWithValue("lessonId", 42L);
        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(LessonNotFoundException.class);
        assertThatThrownBy(() -> service.regenerate(42L)).isInstanceOf(LessonNotFoundException.class);
        verify(adaptationService, never()).adapt(any(), any(), any());
    }

    @Test
    void deleteRemovesLesson() {
        Lesson lesson = existing(LessonStatus.DRAFT);

        service.delete(lesson.getId());

        verify(repository).delete(lesson);
    }

    @Test
    void listsAllProfiles() {
        assertThat(service.listProfiles())
                .extracting(ProfileResponse::code)
                .containsExactly(AdaptationProfile.values());
    }

    private Lesson existing(LessonStatus status) {
        Lesson lesson = new Lesson();
        lesson.setId(7L);
        lesson.setTitle("Растения");
        lesson.setOriginalText(TEXT);
        lesson.setProfile(AdaptationProfile.DYSLEXIA);
        lesson.setStatus(status);
        lesson.setAdaptedContent(validLesson());
        when(repository.findById(7L)).thenReturn(Optional.of(lesson));
        return lesson;
    }
}
