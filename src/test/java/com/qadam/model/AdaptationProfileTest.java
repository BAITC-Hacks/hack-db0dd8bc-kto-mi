package com.qadam.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptationProfileTest {

    @ParameterizedTest
    @EnumSource(AdaptationProfile.class)
    void everyProfileHasDisplayNameAndRules(AdaptationProfile profile) {
        assertThat(profile.getDisplayName()).isNotBlank();
        assertThat(profile.getAdaptationRules())
                .isNotEmpty()
                .allSatisfy(rule -> assertThat(rule).isNotBlank());
    }

    @ParameterizedTest
    @EnumSource(AdaptationProfile.class)
    void everyProfileHasDisplaySettings(AdaptationProfile profile) {
        DisplaySettings settings = profile.getDisplaySettings();

        assertThat(settings).isNotNull();
        assertThat(settings.fontSizePx()).isPositive();
        assertThat(settings.lineHeight()).isPositive();
        assertThat(settings.backgroundColor()).matches("#[0-9A-Fa-f]{6}");
    }

    @Test
    void dyslexiaUsesDyslexiaFont() {
        assertThat(AdaptationProfile.DYSLEXIA.getDisplaySettings().dyslexiaFont()).isTrue();
    }

    @Test
    void autismShowsSequenceStructure() {
        assertThat(AdaptationProfile.AUTISM.getDisplaySettings().showSequenceStructure()).isTrue();
    }
}
