package com.qadam.pictogram;

import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.Card;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.qadam.dto.AdaptedLessonFixtures.validLesson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PictogramEnricherTest {

    private static final String SUN_URL = "https://static.arasaac.org/pictograms/2653/2653_500.png";
    private static final String WATER_URL = "https://static.arasaac.org/pictograms/32464/32464_500.png";

    private final PictogramService pictogramService = mock(PictogramService.class);
    private final PictogramEnricher enricher = new PictogramEnricher(pictogramService);

    @Test
    void setsPictogramUrlForFoundWordsAndNullForOthers() {
        when(pictogramService.isEnabled()).thenReturn(true);
        when(pictogramService.findPictogramUrl("солнце")).thenReturn(Optional.of(SUN_URL));
        when(pictogramService.findPictogramUrl("испарение")).thenReturn(Optional.empty());
        when(pictogramService.findPictogramUrl("вода")).thenReturn(Optional.of(WATER_URL));

        AdaptedLesson lesson = lessonWithCards(
                new Card("солнце", "Солнце греет воду.", null),
                new Card("испарение", "Вода превращается в пар.", null),
                new Card("вода", "Вода есть в реке.", null));

        AdaptedLesson enriched = enricher.enrich(lesson);

        assertThat(enriched.cards()).containsExactly(
                new Card("солнце", "Солнце греет воду.", SUN_URL),
                new Card("испарение", "Вода превращается в пар.", null),
                new Card("вода", "Вода есть в реке.", WATER_URL));
        assertThat(enriched.sentences()).isEqualTo(lesson.sentences());
        assertThat(enriched.quiz()).isEqualTo(lesson.quiz());
    }

    @Test
    void failedLookupLeavesPictogramUrlNull() {
        when(pictogramService.isEnabled()).thenReturn(true);
        when(pictogramService.findPictogramUrl("солнце")).thenThrow(new IllegalStateException("boom"));

        AdaptedLesson enriched = enricher.enrich(lessonWithCards(new Card("солнце", "Солнце греет воду.", null)));

        assertThat(enriched.cards()).singleElement().extracting(Card::pictogramUrl).isNull();
    }

    @Test
    void looksUpCardsInParallel() {
        int cardCount = 3;
        CountDownLatch allStarted = new CountDownLatch(cardCount);
        when(pictogramService.isEnabled()).thenReturn(true);
        when(pictogramService.findPictogramUrl(anyString())).thenAnswer(invocation -> {
            allStarted.countDown();
            // Completes only when every lookup is running at the same time.
            boolean concurrent = allStarted.await(5, TimeUnit.SECONDS);
            return concurrent ? Optional.of("https://example.org/" + invocation.getArgument(0)) : Optional.empty();
        });

        AdaptedLesson enriched = enricher.enrich(lessonWithCards(
                new Card("a", "A.", null), new Card("b", "B.", null), new Card("c", "C.", null)));

        assertThat(enriched.cards()).extracting(Card::pictogramUrl)
                .containsExactly("https://example.org/a", "https://example.org/b", "https://example.org/c");
    }

    @Test
    void disabledServiceLeavesLessonUntouched() {
        when(pictogramService.isEnabled()).thenReturn(false);
        AdaptedLesson lesson = validLesson();

        assertThat(enricher.enrich(lesson)).isSameAs(lesson);
        verify(pictogramService, never()).findPictogramUrl(anyString());
    }

    private static AdaptedLesson lessonWithCards(Card... cards) {
        AdaptedLesson lesson = validLesson();
        return new AdaptedLesson(lesson.sentences(), List.of(cards), lesson.quiz());
    }
}
