package com.qadam.llm.mock;

import com.qadam.dto.ClarifyingQuestion;
import com.qadam.dto.FieldAnswer;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.model.CardField;
import com.qadam.model.Industry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockLlmClientTest {

    private static final String DRAFT = "Мы сеть из 12 кофеен в Алматы. "
            + "Хотим понять, почему в будни после обеда падают продажи. "
            + "Есть выгрузка чеков из кассовой системы за 2 года в Excel.";

    private final MockLlmClient client = new MockLlmClient();

    @Test
    void buildCardUsesOnlySentencesFromTheDraft() {
        TaskCard card = client.buildCard(DRAFT, Industry.HORECA, List.of());

        assertThat(card.context()).isEqualTo("Мы сеть из 12 кофеен в Алматы.");
        assertThat(card.need()).isEqualTo("Хотим понять, почему в будни после обеда падают продажи.");
        assertThat(card.data()).isEqualTo("Есть выгрузка чеков из кассовой системы за 2 года в Excel.");
        assertThat(card.title()).isEqualTo("Хотим понять, почему в будни после обеда падают продажи");
        assertThat(List.of(card.users(), card.constraints(), card.expectedResult(), card.successCriteria(),
                card.contact(), card.interactionFormat())).allMatch(String::isEmpty);
    }

    @Test
    void buildCardAddsAnswersToTheirFieldsAndIgnoresBlankAnswers() {
        TaskCard card = client.buildCard(DRAFT, Industry.HORECA, List.of(
                new FieldAnswer("need", "Нужны рекомендации по акциям."),
                new FieldAnswer("contact", "ops@example.com"),
                new FieldAnswer("users", "   ")));

        assertThat(card.need()).isEqualTo(
                "Хотим понять, почему в будни после обеда падают продажи. Нужны рекомендации по акциям.");
        assertThat(card.contact()).isEqualTo("ops@example.com");
        assertThat(card.users()).isEmpty();
    }

    @Test
    void analyzeAsksAboutMissingFieldsOnly() {
        TaskAnalysis analysis = client.analyze(DRAFT, Industry.HORECA);

        assertThat(analysis.missingFields()).containsExactly("users", "constraints", "expectedResult",
                "successCriteria", "contact", "interactionFormat");
        assertThat(analysis.questions()).extracting(ClarifyingQuestion::field)
                .containsExactlyElementsOf(analysis.missingFields());
        assertThat(analysis.questions()).allMatch(question -> !question.question().isBlank());
    }

    @Test
    void analyzeAlwaysAsksAtLeastThreeQuestions() {
        String fullDraft = DRAFT
                + " Пользователи — управляющие кофейнями и маркетолог."
                + " Ограничение: бюджет на акции не более ста тысяч."
                + " На выходе ждём дашборд с продажами по часам."
                + " Успех — выручка после обеда растёт заметно."
                + " Почта: ops@example.com."
                + " Готовы на онлайн-встречи раз в неделю.";

        TaskAnalysis analysis = client.analyze(fullDraft, Industry.HORECA);

        assertThat(analysis.missingFields()).isEmpty();
        assertThat(analysis.questions()).hasSizeGreaterThanOrEqualTo(TaskAnalysis.MIN_QUESTIONS);
        assertThat(analysis.questions().getFirst().field()).isEqualTo("successCriteria");
    }

    @Test
    void classifiesSentencesByKeywords() {
        assertThat(MockLlmClient.classify("Пишите на team@example.com")).isEqualTo(CardField.CONTACT);
        assertThat(MockLlmClient.classify("Сократить время ожидания на 20%")).isEqualTo(CardField.SUCCESS_CRITERIA);
        assertThat(MockLlmClient.classify("Бюджет ограничен")).isEqualTo(CardField.CONSTRAINTS);
        assertThat(MockLlmClient.classify("Просто текст без подсказок")).isEqualTo(CardField.CONTEXT);
    }

    @Test
    void longTitleIsCutAtWordBoundary() {
        String title = MockLlmClient.title("Слово ".repeat(30) + "конец.");

        assertThat(title).endsWith("…").hasSizeLessThanOrEqualTo(MockLlmClient.TITLE_MAX_LENGTH + 1);
        assertThat(MockLlmClient.title("Короткое название!")).isEqualTo("Короткое название");
    }

    @Test
    void isDeterministic() {
        assertThat(client.analyze(DRAFT, Industry.RETAIL)).isEqualTo(client.analyze(DRAFT, Industry.RETAIL));
        assertThat(client.buildCard(DRAFT, Industry.RETAIL, List.of()))
                .isEqualTo(client.buildCard(DRAFT, Industry.RETAIL, List.of()));
    }
}
