package com.loopin.api.common.logging;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CorrelationIdGenerator {

    public String next() {
        return UUID.randomUUID().toString();
    }
}
