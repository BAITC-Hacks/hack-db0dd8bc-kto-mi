package com.qadam.llm;

import com.qadam.model.AdaptationProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptationPromptTest {

    private final AdaptationPrompt prompt = new AdaptationPrompt();

    @ParameterizedTest
    @EnumSource(AdaptationProfile.class)
    void systemPromptContainsProfileNameAndRules(AdaptationProfile profile) {
        String systemPrompt = prompt.systemPrompt(profile);

        assertThat(systemPrompt)
                .doesNotContain("{{")
                .contains(profile.getDisplayName())
                .contains("strictly in Russian");
        profile.getAdaptationRules().forEach(rule -> assertThat(systemPrompt).contains("- " + rule));
    }

    @Test
    void userPromptContainsTitleAndText() {
        assertThat(prompt.userPrompt("Части растения", "У растения есть корень."))
                .contains("Части растения")
                .contains("У растения есть корень.");
    }
}
