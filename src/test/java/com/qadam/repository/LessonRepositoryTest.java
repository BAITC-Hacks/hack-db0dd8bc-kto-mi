package com.qadam.repository;

import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.Question;
import com.qadam.model.AdaptationProfile;
import com.qadam.model.Lesson;
import com.qadam.model.LessonStatus;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static com.qadam.dto.AdaptedLessonFixtures.validLesson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class LessonRepositoryTest {

    @Autowired
    private LessonRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsLessonWithAdaptedContent() {
        AdaptedLesson content = validLesson();
        Lesson lesson = newLesson(AdaptationProfile.AUTISM);
        lesson.setAdaptedContent(content);

        Long id = repository.save(lesson).getId();
        entityManager.flush();
        entityManager.clear();

        Lesson loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo("Plants");
        assertThat(loaded.getOriginalText()).isEqualTo("Plants grow from seeds.");
        assertThat(loaded.getProfile()).isEqualTo(AdaptationProfile.AUTISM);
        assertThat(loaded.getStatus()).isEqualTo(LessonStatus.DRAFT);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(loaded.getAdaptedContent()).isEqualTo(content);
    }

    @Test
    void savesLessonWithoutAdaptedContent() {
        Long id = repository.save(newLesson(AdaptationProfile.DYSLEXIA)).getId();
        entityManager.flush();
        entityManager.clear();

        Lesson loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getProfile()).isEqualTo(AdaptationProfile.DYSLEXIA);
        assertThat(loaded.getAdaptedContent()).isNull();
    }

    @Test
    void rejectsInvalidAdaptedContent() {
        AdaptedLesson valid = validLesson();
        Question invalidQuestion = new Question("Question?", List.of("A", "B"), 5);
        Lesson lesson = newLesson(AdaptationProfile.DYSLEXIA);
        lesson.setAdaptedContent(new AdaptedLesson(valid.sentences(), valid.cards(), List.of(invalidQuestion)));

        assertThatThrownBy(() -> {
            repository.save(lesson);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    private static Lesson newLesson(AdaptationProfile profile) {
        Lesson lesson = new Lesson();
        lesson.setTitle("Plants");
        lesson.setOriginalText("Plants grow from seeds.");
        lesson.setProfile(profile);
        return lesson;
    }
}
