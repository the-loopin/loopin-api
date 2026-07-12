package com.loopin.api.events.shared.validation;

import java.util.Map;

public class EventRequestValidationException extends IllegalArgumentException {

    private final Map<String, String> fieldErrors;

    public EventRequestValidationException(Map<String, String> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
