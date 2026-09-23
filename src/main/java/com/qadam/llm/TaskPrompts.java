package com.qadam.llm;

import com.qadam.dto.FieldAnswer;
import com.qadam.model.Industry;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * System prompts from {@code prompts/*.md} and user messages for both LLM functions.
 */
public class TaskPrompts {

    static final String ANALYZE_PATH = "prompts/analyze-task.md";
    static final String BUILD_CARD_PATH = "prompts/build-card.md";

    private final String analyzeSystem;
    private final String buildCardSystem;

    public TaskPrompts() {
        this.analyzeSystem = read(ANALYZE_PATH);
        this.buildCardSystem = read(BUILD_CARD_PATH);
    }

    public String analyzeSystemPrompt() {
        return analyzeSystem;
    }

    public String buildCardSystemPrompt() {
        return buildCardSystem;
    }

    public String analyzeUserPrompt(String draftText, Industry industry) {
        return "Industry: " + industry.getDisplayName() + "\n\nDraft:\n" + draftText;
    }

    public String buildCardUserPrompt(String draftText, Industry industry, List<FieldAnswer> answers) {
        String answerLines = answers.stream()
                .filter(answer -> answer.answer() != null && !answer.answer().isBlank())
                .map(answer -> "- " + answer.field() + ": " + answer.answer().strip())
                .collect(Collectors.joining("\n"));
        return analyzeUserPrompt(draftText, industry)
                + "\n\nAnswers to clarifying questions:\n" + (answerLines.isEmpty() ? "(none)" : answerLines);
    }

    private static String read(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read prompt " + path, e);
        }
    }
}
