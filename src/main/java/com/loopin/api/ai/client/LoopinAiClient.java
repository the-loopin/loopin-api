package com.loopin.api.ai.client;

import com.loopin.api.ai.config.LoopinAiProperties;
import com.loopin.api.common.metrics.LoopinOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopin.api.ai.dto.EmbeddingRequest;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.ai.dto.EmbeddingBatchRequest;
import com.loopin.api.ai.dto.EmbeddingBatchResponse;
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

    @LoopinOperation(domain = "ai", operation = "embed_passage")
    public EmbeddingResponse embedPassage(String text) {
        return embed(text, "passage");
    }

    @LoopinOperation(domain = "ai", operation = "embed_query")
    public EmbeddingResponse embedQuery(String text) {
        return embed(text, "query");
    }

    @LoopinOperation(domain = "ai", operation = "rerank")
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
                int status = response.statusCode();
                boolean retryable = status == 408 || status == 429 || status >= 500;
                throw new LoopinAiException("Loopin AI request failed with status " + status,
                        retryable, "AI_HTTP_" + status);
            }

            return objectMapper.readValue(response.body(), responseType);
        } catch (JsonProcessingException exception) {
            throw new LoopinAiException("Invalid Loopin AI payload or response", false,
                    "AI_CONTRACT", exception);
        } catch (IOException exception) {
            throw new LoopinAiException("Loopin AI connection failed", true,
                    "AI_CONNECTION", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LoopinAiException("Loopin AI request was interrupted", true,
                    "AI_INTERRUPTED", exception);
        }
    }

    @LoopinOperation(domain = "ai", operation = "embed_passage_batch")
    public EmbeddingBatchResponse embedPassages(List<String> texts) {
        return post("/v1/embeddings/text/batch",
                new EmbeddingBatchRequest(texts, "passage"), EmbeddingBatchResponse.class);
    }
}
