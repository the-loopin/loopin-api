package com.loopin.api.moderation.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopin.api.moderation.ai.dto.AiModerationRequest;
import com.loopin.api.moderation.ai.dto.AiModerationResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** HTTP adapter for a configured Loopin AI-compatible moderation endpoint. */
@Component
public class HttpAiModerationClient implements AiModerationClient {

    private final AiModerationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpAiModerationClient(AiModerationProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build();
    }

    @Override
    public AiModerationResponse moderate(AiModerationRequest requestPayload) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(buildEndpointUrl()))
                    .timeout(properties.getTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestPayload)));

            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                request.header("X-API-Key", properties.getApiKey());
            }

            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("AI moderation request failed with status " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), AiModerationResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to process AI moderation payload", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("AI moderation request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI moderation request was interrupted", exception);
        }
    }

    private String buildEndpointUrl() {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new IllegalStateException("AI moderation base URL is required when AI moderation is enabled");
        }
        if (properties.getEndpointPath() == null || properties.getEndpointPath().isBlank()) {
            throw new IllegalStateException("AI moderation endpoint path is required when AI moderation is enabled");
        }

        return properties.getBaseUrl().replaceAll("/+$", "")
                + "/"
                + properties.getEndpointPath().replaceFirst("^/+", "");
    }
}
