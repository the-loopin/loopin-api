package com.loopin.api.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "loopin.ai")
public class LoopinAiProperties {

    private String baseUrl = "http://localhost:8000";
    private Duration timeout = Duration.ofSeconds(2);
    private String embeddingModel = "intfloat/multilingual-e5-small";
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

    public EmbeddingJobs getEmbeddingJobs() { return embeddingJobs; }

    public static class EmbeddingJobs {
        private boolean enabled = true;
        private int batchSize = 25;
        private int maxAttempts = 8;
        private Duration initialBackoff = Duration.ofSeconds(5);
        private Duration maxBackoff = Duration.ofMinutes(30);
        private Duration processingTimeout = Duration.ofMinutes(5);
        private double backoffJitter = 0.2;
        private int embeddingDimensions = 384;
        private boolean batchEnabled = true;
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
    }

}
