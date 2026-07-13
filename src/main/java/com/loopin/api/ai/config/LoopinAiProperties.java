package com.loopin.api.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "loopin.ai")
@Validated
public class LoopinAiProperties {

    private String baseUrl = "http://localhost:8000";
    @NotNull
    private Duration timeout = Duration.ofSeconds(2);
    private String embeddingModel = "intfloat/multilingual-e5-small";
    private String serviceToken;
    @Valid
    private final EmbeddingJobs embeddingJobs = new EmbeddingJobs();

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

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getServiceToken() { return serviceToken; }

    public void setServiceToken(String serviceToken) { this.serviceToken = serviceToken; }

    public EmbeddingJobs getEmbeddingJobs() { return embeddingJobs; }

    @AssertTrue(message = "service-token must be configured when embedding jobs are enabled")
    public boolean isServiceTokenConfiguredForWorker() {
        return !embeddingJobs.isEnabled() || StringUtils.hasText(serviceToken);
    }

    @AssertTrue(message = "timeout must be positive")
    public boolean isTimeoutPositive() {
        return isPositive(timeout);
    }

    @AssertTrue(message = "embedding-jobs.processing-timeout must be greater than timeout")
    public boolean isProcessingTimeoutGreaterThanRequestTimeout() {
        return timeout != null && embeddingJobs.getProcessingTimeout() != null
                && embeddingJobs.getProcessingTimeout().compareTo(timeout) > 0;
    }

    public static class EmbeddingJobs {
        private boolean enabled = true;
        @Min(1)
        private int batchSize = 25;
        @Min(1)
        private int maxAttempts = 8;
        @NotNull
        private Duration initialBackoff = Duration.ofSeconds(5);
        @NotNull
        private Duration maxBackoff = Duration.ofMinutes(30);
        @NotNull
        private Duration processingTimeout = Duration.ofMinutes(5);
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double backoffJitter = 0.2;
        @Min(1)
        private int embeddingDimensions = 384;
        private boolean batchEnabled = true;
        @Min(1)
        @Max(128)
        private int aiBatchSize = 32;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public Duration getInitialBackoff() { return initialBackoff; }
        public void setInitialBackoff(Duration initialBackoff) { this.initialBackoff = initialBackoff; }
        public Duration getMaxBackoff() { return maxBackoff; }
        public void setMaxBackoff(Duration maxBackoff) { this.maxBackoff = maxBackoff; }
        public Duration getProcessingTimeout() { return processingTimeout; }
        public void setProcessingTimeout(Duration processingTimeout) { this.processingTimeout = processingTimeout; }
        public double getBackoffJitter() { return backoffJitter; }
        public void setBackoffJitter(double backoffJitter) { this.backoffJitter = backoffJitter; }
        public int getEmbeddingDimensions() { return embeddingDimensions; }
        public void setEmbeddingDimensions(int embeddingDimensions) { this.embeddingDimensions = embeddingDimensions; }
        public boolean isBatchEnabled() { return batchEnabled; }
        public void setBatchEnabled(boolean batchEnabled) { this.batchEnabled = batchEnabled; }
        public int getAiBatchSize() { return aiBatchSize; }
        public void setAiBatchSize(int aiBatchSize) { this.aiBatchSize = aiBatchSize; }

        @AssertTrue(message = "retry and processing durations must be positive")
        public boolean isDurationsPositive() {
            return isPositive(initialBackoff) && isPositive(maxBackoff) && isPositive(processingTimeout);
        }

        @AssertTrue(message = "max-backoff must be greater than or equal to initial-backoff")
        public boolean isBackoffRangeValid() {
            return initialBackoff != null && maxBackoff != null
                    && maxBackoff.compareTo(initialBackoff) >= 0;
        }
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
