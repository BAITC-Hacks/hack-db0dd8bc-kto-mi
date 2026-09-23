package com.qadam.service;

/**
 * The lesson could not be adapted: the LLM is unavailable or kept returning invalid results.
 */
public class LlmAdaptationException extends RuntimeException {

    public LlmAdaptationException(String message, Throwable cause) {
        super(message, cause);
    }
}
