package com.qadam.llm.mock;

import com.qadam.dto.ClarifyingQuestion;
import com.qadam.dto.FieldAnswer;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.llm.LlmClient;
import com.qadam.model.CardField;
import com.qadam.model.Industry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Offline, deterministic {@link LlmClient} for demos, development and tests.
 *
 * <p>Splits the draft into sentences and assigns each sentence to a card field by keywords
 * (sentences without keywords go to {@code context}). Nothing is invented: every card field
 * consists only of sentences from the draft and the business answers, fields without information stay empty.
 */
public class MockLlmClient implements LlmClient {

    static final int TITLE_MAX_LENGTH = 90;

    private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?…])\\s+|\\R+");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s()-]{8,}\\d");
    private static final Pattern HANDLE = Pattern.compile("(?<![\\w.])@[A-Za-z0-9_]{4,}");

    /** Keywords per field, checked in this order: the first matching field wins. */
    private static final Map<CardField, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(CardField.CONTACT, List.of("контакт", "почт", "email", "e-mail", "телефон", "звоните"));
        KEYWORDS.put(CardField.SUCCESS_CRITERIA, List.of("критери", "успех", "успеш", "метрик", "kpi", "%",
                "процент", "сократить", "снизить", "увеличить", "повысить", "уменьшить"));
        KEYWORDS.put(CardField.DATA, List.of("данны", "выгрузк", "excel", "csv", "таблиц", "база", "базу", "архив",
                "api", "логи", "документац", "материал"));
        KEYWORDS.put(CardField.CONSTRAINTS, List.of("огранич", "бюджет", "нельзя", "не более", "не больше", "срок",
                "запрещ", "персональн", "обязательно"));
        KEYWORDS.put(CardField.INTERACTION_FORMAT, List.of("встреч", "созвон", "онлайн", "офлайн", "очно",
                "раз в неделю", "zoom", "чат", "формат работы", "формат взаимодейств"));
        KEYWORDS.put(CardField.USERS, List.of("пользовател", "клиент", "покупател", "сотрудник", "менеджер",
                "оператор", "водител", "пациент", "посетител", "для кого"));
        KEYWORDS.put(CardField.EXPECTED_RESULT, List.of("результат", "прототип", "дашборд", "бот", "приложени",
                "отчет", "на выходе", "mvp", "ожида"));
        KEYWORDS.put(CardField.NEED, List.of("нужн", "хотим", "требуется", "необходим", "проблем", "задача",
                "помогите", "не хватает", "теряем", "тратим", "вручную"));
        KEYWORDS.put(CardField.CONTEXT, List.of("компани", "мы ", "наш", "занимаемся", "работаем", "сеть",
                "фирм", "магазин", "предприят"));
    }

    private static final Map<CardField, String> QUESTIONS = new EnumMap<>(Map.of(
            CardField.TITLE, "Как коротко назвать задачу, чтобы студентам было понятно, о чём она?",
            CardField.CONTEXT, "Расскажите коротко о компании: чем вы занимаетесь и где возникла задача?",
            CardField.NEED, "Какую проблему нужно решить и почему это важно для бизнеса сейчас?",
            CardField.USERS, "Кто будет пользоваться результатом: клиенты, сотрудники, руководители?",
            CardField.DATA, "Какие данные и материалы вы готовы передать команде (выгрузки, документы, доступы)?",
            CardField.CONSTRAINTS, "Есть ли ограничения: сроки, бюджет, обязательные технологии, правила работы с данными?",
            CardField.EXPECTED_RESULT, "Что вы хотите получить в итоге: прототип, отчёт, модель, сервис?",
            CardField.SUCCESS_CRITERIA, "По каким измеримым показателям вы поймёте, что задача решена (цифры, проценты)?",
            CardField.CONTACT, "Как команде связаться с представителем компании (имя, роль, почта или телефон)?",
            CardField.INTERACTION_FORMAT, "Как вы готовы работать с командой: как часто встречаться, онлайн или очно?"));

    @Override
    public TaskAnalysis analyze(String draftText, Industry industry) {
        TaskCard card = extractCard(draftText);
        List<CardField> missing = Arrays.stream(CardField.values())
                .filter(field -> !field.isFilledIn(card))
                .toList();

        List<CardField> asked = new ArrayList<>(missing);
        if (asked.size() < TaskAnalysis.MIN_QUESTIONS && !CardField.isMeasurable(card.successCriteria())
                && !asked.contains(CardField.SUCCESS_CRITERIA)) {
            asked.add(CardField.SUCCESS_CRITERIA);
        }
        Arrays.stream(CardField.values())
                .filter(field -> field != CardField.TITLE && !asked.contains(field))
                .sorted(Comparator.comparingInt(field -> field.valueIn(card).length()))
                .limit(Math.max(0, TaskAnalysis.MIN_QUESTIONS - asked.size()))
                .forEach(asked::add);

        return new TaskAnalysis(
                missing.stream().map(CardField::getCode).toList(),
                asked.stream().map(field -> new ClarifyingQuestion(field.getCode(), QUESTIONS.get(field))).toList());
    }

    @Override
    public TaskCard buildCard(String draftText, Industry industry, List<FieldAnswer> answers) {
        Map<CardField, String> values = extract(draftText);
        for (FieldAnswer answer : answers) {
            if (answer.answer() == null || answer.answer().isBlank()) {
                continue;
            }
            CardField.fromCode(answer.field()).ifPresent(field -> values.merge(field, answer.answer().strip(),
                    (existing, added) -> field == CardField.TITLE || existing.isEmpty() ? added : existing + " " + added));
        }
        return toCard(values);
    }

    static TaskCard extractCard(String draftText) {
        return toCard(extract(draftText));
    }

    private static Map<CardField, String> extract(String draftText) {
        Map<CardField, String> values = new EnumMap<>(CardField.class);
        for (CardField field : CardField.values()) {
            values.put(field, "");
        }
        List<String> sentences = SENTENCE_BREAK.splitAsStream(draftText == null ? "" : draftText)
                .map(String::strip)
                .filter(sentence -> !sentence.isEmpty())
                .toList();
        for (String sentence : sentences) {
            values.merge(classify(sentence), sentence,
                    (existing, added) -> existing.isEmpty() ? added : existing + " " + added);
        }
        String titleSource = values.get(CardField.NEED).isEmpty()
                ? sentences.stream().findFirst().orElse("")
                : firstSentence(values.get(CardField.NEED), sentences);
        values.put(CardField.TITLE, title(titleSource));
        return values;
    }

    static CardField classify(String sentence) {
        if (EMAIL.matcher(sentence).find() || PHONE.matcher(sentence).find() || HANDLE.matcher(sentence).find()) {
            return CardField.CONTACT;
        }
        String normalized = sentence.toLowerCase(Locale.ROOT).replace('ё', 'е');
        return KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(normalized::contains))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(CardField.CONTEXT);
    }

    private static String firstSentence(String fieldValue, List<String> sentences) {
        return sentences.stream().filter(fieldValue::startsWith).findFirst().orElse(fieldValue);
    }

    /** The sentence without the final punctuation, cut at a word boundary if it is too long. */
    static String title(String sentence) {
        String title = sentence.strip().replaceAll("[.!?…]+$", "");
        if (title.length() <= TITLE_MAX_LENGTH) {
            return title;
        }
        int cut = title.lastIndexOf(' ', TITLE_MAX_LENGTH);
        return title.substring(0, cut > 0 ? cut : TITLE_MAX_LENGTH).strip() + "…";
    }

    private static TaskCard toCard(Map<CardField, String> values) {
        return new TaskCard(
                values.get(CardField.TITLE),
                values.get(CardField.CONTEXT),
                values.get(CardField.NEED),
                values.get(CardField.USERS),
                values.get(CardField.DATA),
                values.get(CardField.CONSTRAINTS),
                values.get(CardField.EXPECTED_RESULT),
                values.get(CardField.SUCCESS_CRITERIA),
                values.get(CardField.CONTACT),
                values.get(CardField.INTERACTION_FORMAT));
    }
}
