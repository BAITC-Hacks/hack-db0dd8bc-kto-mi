package com.qadam.service;

import lombok.Getter;

/**
 * The action is not allowed in the current state, e.g. a proposal for an unpublished task.
 */
@Getter
public class InvalidStateException extends RuntimeException {

    /** Message for the user, in Russian. */
    private final String userMessage;

    public InvalidStateException(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage;
    }
}
