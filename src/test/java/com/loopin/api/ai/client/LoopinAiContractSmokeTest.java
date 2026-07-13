package com.loopin.api.ai.client;

import com.loopin.api.ai.config.LoopinAiProperties;
import com.loopin.api.ai.dto.EmbeddingBatchResponse;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.common.logging.CorrelationIdFilter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.MDC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ai-contract")
@EnabledIfEnvironmentVariable(named = "LOOPIN_AI_CONTRACT_TEST_ENABLED", matches = "(?i)true")
class LoopinAiContractSmokeTest {

    @Test
    void authenticatedJavaClientMatchesRealSingleAndBatchEmbeddingContract() throws Exception {
        String baseUrl = environment("LOOPIN_AI_BASE_URL", "http://localhost:8000");
        String token = requiredEnvironment("LOOPIN_AI_SERVICE_TOKEN");
        String model = environment("LOOPIN_AI_EMBEDDING_MODEL", "intfloat/multilingual-e5-small");
        int dimensions = Integer.parseInt(environment("LOOPIN_AI_EMBEDDING_DIMENSIONS", "384"));
        waitUntilReady(baseUrl, token);

        LoopinAiProperties properties = new LoopinAiProperties();
        properties.setBaseUrl(baseUrl);
        properties.setTimeout(Duration.ofSeconds(10));
        properties.setEmbeddingModel(model);
        properties.setServiceToken(token);
        LoopinAiClient client = new LoopinAiClient(properties);

        MDC.put(CorrelationIdFilter.MDC_KEY, "loopin-ai-contract-smoke-123");
        try {
            EmbeddingResponse single = client.embedPassage("Loopin contract smoke test");
            EmbeddingBatchResponse batch = client.embedPassages(List.of(
                    "Loopin contract batch item one", "Loopin contract batch item two"));

            assertEmbedding(single.model(), single.dimensions(), single.embedding(), model, dimensions);
            assertThat(batch.model()).isEqualTo(model);
            assertThat(batch.dimensions()).isEqualTo(dimensions);
            assertThat(batch.embeddings()).hasSize(2)
                    .allSatisfy(vector -> assertValidVector(vector, dimensions));
        } finally {
            MDC.clear();
        }
    }

    private void waitUntilReady(String baseUrl, String token) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        Instant deadline = Instant.now().plusSeconds(60);
        while (Instant.now().isBefore(deadline)) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/ready"))
                        .timeout(Duration.ofSeconds(2))
                        .header("Authorization", "Bearer " + token)
                        .header(CorrelationIdFilter.HEADER_NAME, "loopin-ai-contract-ready-123")
                        .GET().build();
                if (client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) return;
            } catch (java.io.IOException ignored) {
                // The opt-in target may still be starting.
            }
            Thread.sleep(1000);
        }
        throw new AssertionError("loopin-ai did not become ready within 60 seconds");
    }

    private void assertEmbedding(String actualModel, int actualDimensions, List<Double> vector,
                                 String expectedModel, int expectedDimensions) {
        assertThat(actualModel).isEqualTo(expectedModel);
        assertThat(actualDimensions).isEqualTo(expectedDimensions);
        assertValidVector(vector, expectedDimensions);
    }

    private void assertValidVector(List<Double> vector, int dimensions) {
        assertThat(vector).hasSize(dimensions).allSatisfy(value -> assertThat(value).isFinite());
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new AssertionError(name + " must be configured");
        return value;
    }

    private String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
