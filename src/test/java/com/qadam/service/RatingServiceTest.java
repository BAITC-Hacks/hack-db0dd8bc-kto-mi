package com.qadam.service;

import com.qadam.dto.Rating;
import com.qadam.dto.Rating.RatingItem;
import com.qadam.dto.TaskCard;
import com.qadam.model.RatingLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RatingServiceTest {

    private static final String TEXT = "Достаточно длинное описание поля";

    private final RatingService ratingService = new RatingService();

    @Test
    void fullCardWithMeasurableCriteriaGetsAllPoints() {
        Rating rating = ratingService.rate(fullCard());

        assertThat(rating.score()).isEqualTo(100);
        assertThat(rating.level()).isEqualTo(RatingLevel.PRIORITY);
        assertThat(rating.levelName()).isEqualTo("Приоритетная");
        assertThat(rating.missing()).isEmpty();
        assertThat(rating.tips()).isEmpty();
        assertThat(rating.breakdown()).extracting(RatingItem::criterion).containsExactly(
                "Контекст и потребность", "Данные и материалы", "Ожидаемый результат", "Критерии успеха",
                "Ограничения", "Пользователи", "Связь с бизнесом");
        assertThat(rating.breakdown()).allMatch(item -> item.points() == item.maxPoints());
        assertThat(rating.breakdown()).extracting(RatingItem::maxPoints).containsExactly(20, 20, 15, 15, 10, 10, 10);
    }

    @Test
    void emptyCardScoresZero() {
        Rating rating = ratingService.rate(TaskCard.empty());

        assertThat(rating.score()).isZero();
        assertThat(rating.level()).isEqualTo(RatingLevel.DRAFT);
        assertThat(rating.missing()).containsExactly("context", "need", "data", "expectedResult",
                "successCriteria", "constraints", "users", "contact", "interactionFormat");
        assertThat(rating.tips()).hasSize(9).contains("Заполните «Данные и материалы»: +20 баллов");
        assertThat(rating.breakdown()).allMatch(item -> item.points() == 0);
    }

    @ParameterizedTest(name = "{0} is worth {1} points")
    @CsvSource({
            "context, 10",
            "need, 10",
            "data, 20",
            "expectedResult, 15",
            "successCriteria, 15",
            "constraints, 10",
            "users, 10",
            "contact, 5",
            "interactionFormat, 5"
    })
    void eachCriterionIsWorthItsPoints(String field, int points) {
        Rating rating = ratingService.rate(fullCardWithout(field));

        assertThat(rating.score()).isEqualTo(100 - points);
        assertThat(rating.missing()).containsExactly(field);
        assertThat(rating.tips()).singleElement().asString()
                .endsWith("+" + points + " " + RatingService.pointsWord(points));
    }

    @Test
    void combinedCriteriaExplainEachPart() {
        Rating rating = ratingService.rate(fullCardWithout("need"));

        RatingItem item = rating.breakdown().getFirst();
        assertThat(item.points()).isEqualTo(10);
        assertThat(item.maxPoints()).isEqualTo(20);
        assertThat(item.reason()).isEqualTo("Контекст: заполнено (+10); Потребность: не заполнено (0 из 10)");
    }

    @Test
    void successCriteriaWithoutNumbersGetHalfPoints() {
        Rating rating = ratingService.rate(withSuccessCriteria("Клиенты довольны новым сервисом"));

        assertThat(rating.score()).isEqualTo(92);
        assertThat(item(rating, "Критерии успеха").points()).isEqualTo(7);
        assertThat(rating.missing()).isEmpty();
        assertThat(rating.tips()).containsExactly(
                "Добавьте в «Критерии успеха» измеримый показатель — число или процент: +8 баллов");
    }

    @Test
    void successCriteriaWithNumberOrPercentAreMeasurable() {
        assertThat(ratingService.rate(withSuccessCriteria("Время ответа меньше 2 минут")).score()).isEqualTo(100);
        assertThat(ratingService.rate(withSuccessCriteria("Рост конверсии на десять %")).score()).isEqualTo(100);
    }

    @Test
    void shortFieldsDoNotCount() {
        TaskCard card = new TaskCard("Задача", "Кофейни", TEXT, TEXT, TEXT, TEXT, TEXT, TEXT + " 10%", "a@b", TEXT);

        Rating rating = ratingService.rate(card);

        assertThat(rating.missing()).containsExactly("context", "contact");
        assertThat(rating.score()).isEqualTo(100 - 10 - 5);
        assertThat(item(rating, "Контекст и потребность").reason())
                .startsWith("Контекст: слишком коротко: нужно не меньше 15 символов");
    }

    @Test
    void fieldOfExactlyMinimumLengthCounts() {
        String fifteen = "123456789012345";
        TaskCard card = new TaskCard("", fifteen, fifteen.substring(1), "", "", "", "", "", "12345", "");

        Rating rating = ratingService.rate(card);

        assertThat(rating.missing()).doesNotContain("context", "contact").contains("need");
        assertThat(rating.score()).isEqualTo(10 + 5);
    }

    @Test
    void blankFieldIsNotFilled() {
        TaskCard card = new TaskCard(null, "                     ", null, null, null, null, null, null, null, null);

        assertThat(ratingService.rate(card).missing()).contains("context");
    }

    @ParameterizedTest(name = "score {0} → {1}")
    @CsvSource({
            "0, DRAFT", "39, DRAFT",
            "40, WORKING", "69, WORKING",
            "70, READY", "89, READY",
            "90, PRIORITY", "100, PRIORITY"
    })
    void levelBoundaries(int score, RatingLevel level) {
        assertThat(RatingLevel.of(score)).isEqualTo(level);
    }

    @Test
    void levelBoundariesOnRealCards() {
        // context + need + data = 40 → WORKING; without data = 20 → DRAFT
        TaskCard working = new TaskCard("", TEXT, TEXT, "", TEXT, "", "", "", "", "");
        // 100 - users(10) - constraints(10) - contact(5) - format(5) = 70 → READY
        TaskCard ready = new TaskCard("", TEXT, TEXT, "", TEXT, "", TEXT, TEXT + " 5%", "", "");
        // 100 - users(10) = 90 → PRIORITY
        TaskCard priority = fullCardWithout("users");

        assertThat(ratingService.rate(fullCardWithout("data")).level()).isEqualTo(RatingLevel.READY);
        assertThat(ratingService.rate(new TaskCard("", TEXT, TEXT, "", "", "", "", "", "", "")).level())
                .isEqualTo(RatingLevel.DRAFT);
        assertThat(ratingService.rate(working).score()).isEqualTo(40);
        assertThat(ratingService.rate(working).level()).isEqualTo(RatingLevel.WORKING);
        assertThat(ratingService.rate(ready).score()).isEqualTo(70);
        assertThat(ratingService.rate(ready).level()).isEqualTo(RatingLevel.READY);
        assertThat(ratingService.rate(priority).score()).isEqualTo(90);
        assertThat(ratingService.rate(priority).level()).isEqualTo(RatingLevel.PRIORITY);
    }

    @Test
    void russianPluralOfPoints() {
        assertThat(RatingService.pointsWord(1)).isEqualTo("балл");
        assertThat(RatingService.pointsWord(5)).isEqualTo("баллов");
        assertThat(RatingService.pointsWord(8)).isEqualTo("баллов");
        assertThat(RatingService.pointsWord(10)).isEqualTo("баллов");
        assertThat(RatingService.pointsWord(22)).isEqualTo("балла");
        assertThat(RatingService.pointsWord(11)).isEqualTo("баллов");
    }

    private static RatingItem item(Rating rating, String criterion) {
        return rating.breakdown().stream().filter(item -> item.criterion().equals(criterion)).findFirst().orElseThrow();
    }

    static TaskCard fullCard() {
        return new TaskCard("Название задачи", TEXT, TEXT, TEXT, TEXT, TEXT, TEXT,
                "Время обработки заявки сокращается на 30%", "team@example.com", TEXT);
    }

    private static TaskCard withSuccessCriteria(String criteria) {
        TaskCard card = fullCard();
        return new TaskCard(card.title(), card.context(), card.need(), card.users(), card.data(), card.constraints(),
                card.expectedResult(), criteria, card.contact(), card.interactionFormat());
    }

    private static TaskCard fullCardWithout(String field) {
        TaskCard c = fullCard();
        return new TaskCard(
                c.title(),
                field.equals("context") ? "" : c.context(),
                field.equals("need") ? "" : c.need(),
                field.equals("users") ? "" : c.users(),
                field.equals("data") ? "" : c.data(),
                field.equals("constraints") ? "" : c.constraints(),
                field.equals("expectedResult") ? "" : c.expectedResult(),
                field.equals("successCriteria") ? "" : c.successCriteria(),
                field.equals("contact") ? "" : c.contact(),
                field.equals("interactionFormat") ? "" : c.interactionFormat());
    }
}
