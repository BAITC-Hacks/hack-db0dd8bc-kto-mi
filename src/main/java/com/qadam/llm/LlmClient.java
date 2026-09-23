package com.qadam.llm;

import com.qadam.dto.FieldAnswer;
import com.qadam.dto.TaskAnalysis;
import com.qadam.dto.TaskCard;
import com.qadam.model.Industry;

import java.util.List;

/**
 * Turns a business draft into a structured task card using a language model.
 * Both methods throw {@link LlmInvalidResponseException} if the model answer is unusable
 * and {@link LlmException} if the model could not be called at all.
 */
public interface LlmClient {

    /**
     * Finds the card fields the draft does not cover and asks clarifying questions about them.
     */
    TaskAnalysis analyze(String draftText, Industry industry);

    /**
     * Builds the card strictly from the draft and the answers; unknown information stays an empty string.
     */
    TaskCard buildCard(String draftText, Industry industry, List<FieldAnswer> answers);
}
