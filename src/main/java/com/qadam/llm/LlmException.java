package com.qadam.llm;

/**
 * The language model could not be called, e.g. a network error or an HTTP error status.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
