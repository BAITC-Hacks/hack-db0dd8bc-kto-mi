package com.qadam.config;

import com.qadam.model.AdaptationProfile;
import com.qadam.model.Lesson;
import com.qadam.model.LessonStatus;
import com.qadam.repository.LessonRepository;
import com.qadam.service.DemoLessons;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Uses its own in-memory database: other test contexts share {@code jdbc:h2:mem:qadam} and may leave lessons there.
 */
@SpringBootTest(properties = {
        "qadam.demo-data.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:demo-data-test;DB_CLOSE_DELAY=-1"
})
class DemoDataLoaderTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private DemoDataLoader demoDataLoader;

    @Test
    void createsApprovedDemoLessonsOnceOnStartup() {
        assertThat(lessonRepository.findAll())
                .extracting(Lesson::getTitle, Lesson::getProfile, Lesson::getStatus)
                .containsExactlyInAnyOrder(
                        tuple(DemoLessons.WATER_CYCLE_TITLE, AdaptationProfile.DYSLEXIA, LessonStatus.APPROVED),
                        tuple(DemoLessons.PLANT_PARTS_TITLE, AdaptationProfile.AUTISM, LessonStatus.APPROVED));
        assertThat(lessonRepository.findAll())
                .allSatisfy(lesson -> assertThat(lesson.getAdaptedContent()).isNotNull());

        demoDataLoader.run(new DefaultApplicationArguments());

        assertThat(lessonRepository.count()).isEqualTo(2);
    }
}
