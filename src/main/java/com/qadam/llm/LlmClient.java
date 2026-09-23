package com.qadam.llm;

import com.qadam.dto.AdaptedLesson;
import com.qadam.model.AdaptationProfile;

/**
 * Adapts lesson text for a given profile using a language model.
 */
public interface LlmClient {

    /**
     * @throws LlmInvalidResponseException if the model response cannot be turned into an {@link AdaptedLesson}
     * @throws LlmException                if the model could not be called at all
     */
    AdaptedLesson adapt(String title, String text, AdaptationProfile profile);
}
