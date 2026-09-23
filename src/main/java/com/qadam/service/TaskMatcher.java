package com.qadam.service;

import com.qadam.dto.TaskCard;
import com.qadam.model.Task;
import com.qadam.model.Team;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple word matching between a team profile and a task text, without AI.
 *
 * <p>A team keyword (interest, skill or technology) matches when any of its words shares a stem with a word
 * of the task text. The stem is the first {@value #STEM_LENGTH} letters, so «аналитика» matches «анализ».
 * Words shorter than {@value #MIN_WORD_LENGTH} letters and a few stop words are ignored.
 */
@Component
public class TaskMatcher {

    static final int STEM_LENGTH = 5;
    static final int MIN_WORD_LENGTH = 2;

    private static final Pattern WORD_BREAK = Pattern.compile("[^\\p{L}\\p{N}+#]+");
    private static final Set<String> STOP_WORDS = Set.of("по", "на", "из", "не", "от", "до", "за", "во", "со",
            "об", "мы", "вы", "их", "то", "же", "бы", "ли", "для", "при", "как", "что", "это", "или", "над", "под",
            "все", "and", "the", "for", "of", "to", "in");

    public record Match(int score, List<String> matchedKeywords) {
    }

    public Match match(Team team, Task task) {
        Set<String> taskStems = stems(taskText(task));
        List<String> matched = Stream.of(team.getInterests(), team.getSkills(), team.getTechnologies())
                .flatMap(List::stream)
                .map(String::strip)
                .filter(keyword -> !keyword.isEmpty())
                .distinct()
                .filter(keyword -> stems(keyword).stream().anyMatch(taskStems::contains))
                .toList();
        return new Match(matched.size(), matched);
    }

    static String taskText(Task task) {
        TaskCard card = task.getCard();
        return String.join(" ", task.getIndustry().getDisplayName(), card.title(), card.context(), card.need(),
                card.users(), card.data(), card.constraints(), card.expectedResult(), card.successCriteria());
    }

    static Set<String> stems(String text) {
        return Arrays.stream(WORD_BREAK.split(text.toLowerCase(Locale.ROOT).replace('ё', 'е')))
                .filter(word -> word.length() >= MIN_WORD_LENGTH && !STOP_WORDS.contains(word))
                .map(word -> word.length() > STEM_LENGTH ? word.substring(0, STEM_LENGTH) : word)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
