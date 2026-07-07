package com.loopin.api.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopin.api.ai.dto.EmbeddingRequest;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.ai.dto.RerankCandidate;
import com.loopin.api.ai.dto.RerankRequest;
import com.loopin.api.ai.dto.RerankResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class LoopinAiClient {

    private final LoopinAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LoopinAiClient(LoopinAiProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build();
    }

    public EmbeddingResponse embedPassage(String text) {
        return embed(text, "passage");
    }

    public EmbeddingResponse embedQuery(String text) {
        return embed(text, "query");
    }

    public RerankResponse rerank(String query, List<RerankCandidate> candidates, int topK) {
        return post("/v1/rerank", new RerankRequest(query, candidates, topK), RerankResponse.class);
    }

    private EmbeddingResponse embed(String text, String inputType) {
        return post("/v1/embeddings/text", new EmbeddingRequest(text, inputType), EmbeddingResponse.class);
    }

    private <T> T post(String path, Object payload, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + path))
                    .timeout(properties.getTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Loopin AI request failed with status " + response.statusCode());
            }

            return objectMapper.readValue(response.body(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Loopin AI request", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Loopin AI request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Loopin AI request was interrupted", exception);
        }
    }
}
