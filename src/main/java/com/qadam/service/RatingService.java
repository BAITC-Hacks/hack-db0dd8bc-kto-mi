package com.qadam.service;

import com.qadam.dto.Rating;
import com.qadam.dto.Rating.RatingItem;
import com.qadam.dto.TaskCard;
import com.qadam.model.CardField;
import com.qadam.model.RatingLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic, transparent rating of a task card (no AI), 100 points in total:
 * <ul>
 *     <li>Контекст и потребность — 20 (context 10 + need 10)</li>
 *     <li>Данные и материалы — 20</li>
 *     <li>Ожидаемый результат — 15</li>
 *     <li>Критерии успеха — 15 if they contain a number or a percentage, otherwise 7 (half, rounded down)</li>
 *     <li>Ограничения — 10</li>
 *     <li>Пользователи — 10</li>
 *     <li>Связь с бизнесом — 10 (contact 5 + interactionFormat 5)</li>
 * </ul>
 * A field counts only if {@link CardField#isFilled filled meaningfully}: at least 15 characters, 5 for the contact.
 */
@Service
public class RatingService {

    static final int CONTEXT_POINTS = 10;
    static final int NEED_POINTS = 10;
    static final int DATA_POINTS = 20;
    static final int RESULT_POINTS = 15;
    static final int CRITERIA_POINTS = 15;
    static final int CRITERIA_NOT_MEASURABLE_POINTS = CRITERIA_POINTS / 2;
    static final int CONSTRAINTS_POINTS = 10;
    static final int USERS_POINTS = 10;
    static final int CONTACT_POINTS = 5;
    static final int FORMAT_POINTS = 5;

    public Rating rate(TaskCard card) {
        Accumulator acc = new Accumulator(card);

        acc.combined("Контекст и потребность",
                new Part(CardField.CONTEXT, CONTEXT_POINTS), new Part(CardField.NEED, NEED_POINTS));
        acc.single("Данные и материалы", CardField.DATA, DATA_POINTS);
        acc.single("Ожидаемый результат", CardField.EXPECTED_RESULT, RESULT_POINTS);
        acc.successCriteria();
        acc.single("Ограничения", CardField.CONSTRAINTS, CONSTRAINTS_POINTS);
        acc.single("Пользователи", CardField.USERS, USERS_POINTS);
        acc.combined("Связь с бизнесом",
                new Part(CardField.CONTACT, CONTACT_POINTS), new Part(CardField.INTERACTION_FORMAT, FORMAT_POINTS));

        RatingLevel level = RatingLevel.of(acc.score);
        return new Rating(acc.score, level, level.getDisplayName(), List.copyOf(acc.breakdown),
                List.copyOf(acc.missing), List.copyOf(acc.tips));
    }

    private record Part(CardField field, int points) {
    }

    private static final class Accumulator {

        private final TaskCard card;
        private final List<RatingItem> breakdown = new ArrayList<>();
        private final List<String> missing = new ArrayList<>();
        private final List<String> tips = new ArrayList<>();
        private int score;

        private Accumulator(TaskCard card) {
            this.card = card;
        }

        void single(String criterion, CardField field, int maxPoints) {
            boolean filled = field.isFilledIn(card);
            if (!filled) {
                markMissing(field, maxPoints);
            }
            add(criterion, filled ? maxPoints : 0, maxPoints, filled ? "Заполнено" : notFilledReason(field));
        }

        void combined(String criterion, Part first, Part second) {
            int points = 0;
            List<String> reasons = new ArrayList<>();
            for (Part part : List.of(first, second)) {
                boolean filled = part.field().isFilledIn(card);
                if (filled) {
                    points += part.points();
                } else {
                    markMissing(part.field(), part.points());
                }
                reasons.add(part.field().getLabel() + ": " + (filled
                        ? "заполнено (+" + part.points() + ")"
                        : notFilledReason(part.field()).toLowerCase(Locale.ROOT) +" (0 из " + part.points() + ")"));
            }
            add(criterion, points, first.points() + second.points(), String.join("; ", reasons));
        }

        void successCriteria() {
            CardField field = CardField.SUCCESS_CRITERIA;
            if (!field.isFilledIn(card)) {
                markMissing(field, CRITERIA_POINTS);
                add("Критерии успеха", 0, CRITERIA_POINTS, notFilledReason(field));
            } else if (CardField.isMeasurable(card.successCriteria())) {
                add("Критерии успеха", CRITERIA_POINTS, CRITERIA_POINTS, "Заполнено, есть измеримый показатель");
            } else {
                int gain = CRITERIA_POINTS - CRITERIA_NOT_MEASURABLE_POINTS;
                tips.add("Добавьте в «" + field.getLabel() + "» измеримый показатель — число или процент: +"
                        + gain + " " + pointsWord(gain));
                add("Критерии успеха", CRITERIA_NOT_MEASURABLE_POINTS, CRITERIA_POINTS,
                        "Заполнено, но нет измеримого показателя (числа или процента) — половина баллов");
            }
        }

        private void markMissing(CardField field, int points) {
            missing.add(field.getCode());
            tips.add("Заполните «" + field.getLabel() + "»: +" + points + " " + pointsWord(points));
        }

        private void add(String criterion, int points, int maxPoints, String reason) {
            score += points;
            breakdown.add(new RatingItem(criterion, points, maxPoints, reason));
        }

        private String notFilledReason(CardField field) {
            String value = field.valueIn(card);
            if (value.isEmpty()) {
                return "Не заполнено";
            }
            return "Слишком коротко: нужно не меньше " + field.getMinLength() + " символов";
        }
    }

    /** Russian plural of «балл»: 1 балл, 2 балла, 5 баллов. */
    static String pointsWord(int points) {
        int lastTwo = points % 100;
        int last = points % 10;
        if (lastTwo >= 11 && lastTwo <= 14) {
            return "баллов";
        }
        if (last == 1) {
            return "балл";
        }
        return last >= 2 && last <= 4 ? "балла" : "баллов";
    }
}
