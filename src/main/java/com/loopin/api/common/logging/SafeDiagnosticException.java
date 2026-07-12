package com.loopin.api.common.logging;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A log-only throwable that retains exception types and application stack frames
 * without retaining potentially sensitive exception messages or payloads.
 */
public final class SafeDiagnosticException extends RuntimeException {

    private static final int MAX_CAUSE_DEPTH = 8;

    private SafeDiagnosticException(String exceptionType, Throwable cause) {
        super(exceptionType, cause, false, true);
    }

    public static SafeDiagnosticException from(Throwable throwable) {
        return sanitize(throwable, new IdentityHashMap<>(), 0);
    }

    private static SafeDiagnosticException sanitize(
        Throwable throwable,
        Map<Throwable, Boolean> visited,
        int depth
    ) {
        String exceptionType = throwable.getClass().getName();
        if (depth >= MAX_CAUSE_DEPTH || visited.put(throwable, Boolean.TRUE) != null) {
            return new SafeDiagnosticException(exceptionType + " (cause omitted)", null);
        }

        Throwable cause = throwable.getCause();
        SafeDiagnosticException sanitizedCause = cause == null
            ? null
            : sanitize(cause, visited, depth + 1);
        SafeDiagnosticException sanitized = new SafeDiagnosticException(exceptionType, sanitizedCause);
        sanitized.setStackTrace(throwable.getStackTrace());
        return sanitized;
    }
}
