package com.loopin.api.ai.client;

public class LoopinAiException extends RuntimeException {
    private final boolean transientFailure;
    private final String errorCode;

    public LoopinAiException(String message, boolean transientFailure) {
        this(message, transientFailure, transientFailure ? "AI_TRANSIENT" : "AI_PERMANENT", null);
    }

    public LoopinAiException(String message, boolean transientFailure, Throwable cause) {
        this(message, transientFailure, transientFailure ? "AI_TRANSIENT" : "AI_PERMANENT", cause);
    }

    public LoopinAiException(String message, boolean transientFailure, String errorCode) {
        this(message, transientFailure, errorCode, null);
    }

    public LoopinAiException(String message, boolean transientFailure, String errorCode, Throwable cause) {
        super(message, cause);
        this.transientFailure = transientFailure;
        this.errorCode = errorCode;
    }

    public boolean isTransientFailure() { return transientFailure; }
    public String getErrorCode() { return errorCode; }
}
