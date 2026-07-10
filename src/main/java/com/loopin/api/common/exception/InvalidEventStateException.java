package com.loopin.api.common.exception;

/**
 * Thrown when an operation attempts to mutate an event that is in a terminal
 * lifecycle state ({@code CANCELLED} or {@code COMPLETED}) through the normal
 * update command.  The global exception handler maps this to HTTP 409 Conflict.
 */
public class InvalidEventStateException extends RuntimeException {
    public InvalidEventStateException(String message) {
        super(message);
    }
}
