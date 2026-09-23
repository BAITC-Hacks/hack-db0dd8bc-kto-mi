package com.qadam.llm;

/**
 * The language model answered, but the answer is not a usable adapted lesson:
 * unparseable JSON, a refusal or a truncated response. Such calls are worth retrying.
 */
public class LlmInvalidResponseException extends LlmException {

    public LlmInvalidResponseException(String message) {
        super(message);
    }

    public LlmInvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
