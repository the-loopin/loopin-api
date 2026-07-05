package com.loopin.api.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "loopin.ai")
public class LoopinAiProperties {

    private String baseUrl = "http://localhost:8000";
    private Duration timeout = Duration.ofSeconds(10);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
