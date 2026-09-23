package com.qadam.pictogram;

import com.qadam.dto.AdaptedLesson;
import com.qadam.dto.Card;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sets {@link Card#pictogramUrl()} on every card of an adapted lesson. Cards are looked up in parallel
 * on virtual threads; a card without a pictogram keeps {@code pictogramUrl = null}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictogramEnricher {

    private final PictogramService pictogramService;

    public AdaptedLesson enrich(AdaptedLesson lesson) {
        if (!pictogramService.isEnabled()) {
            return lesson;
        }
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Card>> futures = lesson.cards().stream()
                    .map(card -> CompletableFuture
                            .supplyAsync(() -> withPictogram(card), executor)
                            .exceptionally(e -> {
                                log.warn("Pictogram lookup for card '{}' failed: {}", card.word(), e.getMessage());
                                return withUrl(card, null);
                            }))
                    .toList();
            List<Card> cards = futures.stream().map(CompletableFuture::join).toList();
            return new AdaptedLesson(lesson.sentences(), cards, lesson.quiz());
        }
    }

    private Card withPictogram(Card card) {
        return withUrl(card, pictogramService.findPictogramUrl(card.word()).orElse(null));
    }

    private static Card withUrl(Card card, String pictogramUrl) {
        return new Card(card.word(), card.explanation(), pictogramUrl);
    }
}
