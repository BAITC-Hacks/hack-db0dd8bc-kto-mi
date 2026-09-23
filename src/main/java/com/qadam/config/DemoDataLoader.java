package com.qadam.config;

import com.qadam.dto.CreateLessonRequest;
import com.qadam.model.AdaptationProfile;
import com.qadam.repository.LessonRepository;
import com.qadam.service.DemoLessons;
import com.qadam.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fills an empty database with two approved demo lessons, adapted through the regular {@link LessonService}.
 * Disabled with {@code qadam.demo-data.enabled=false}. A failed adaptation is logged and does not stop the app.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "qadam.demo-data.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    static final List<CreateLessonRequest> DEMO_LESSONS = List.of(
            new CreateLessonRequest(DemoLessons.WATER_CYCLE_TITLE, DemoLessons.WATER_CYCLE_TEXT,
                    AdaptationProfile.DYSLEXIA),
            new CreateLessonRequest(DemoLessons.PLANT_PARTS_TITLE, DemoLessons.PLANT_PARTS_TEXT,
                    AdaptationProfile.AUTISM));

    private final LessonRepository lessonRepository;
    private final LessonService lessonService;

    @Override
    public void run(ApplicationArguments args) {
        if (lessonRepository.count() > 0) {
            log.info("Database already has lessons, demo data skipped");
            return;
        }
        for (CreateLessonRequest request : DEMO_LESSONS) {
            try {
                lessonService.approve(lessonService.create(request).id());
                log.info("Demo lesson '{}' ({}) created and approved", request.title(), request.profile());
            } catch (RuntimeException e) {
                log.warn("Failed to create demo lesson '{}': {}", request.title(), e.getMessage());
            }
        }
    }
}
