package com.qadam.service;

/**
 * The AI could not process the task: the LLM is unavailable or kept returning invalid results.
 */
public class LlmFailureException extends RuntimeException {

    public LlmFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
