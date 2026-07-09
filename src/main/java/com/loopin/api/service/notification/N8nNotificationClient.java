package com.loopin.api.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopin.api.config.N8nNotificationProperties;
import com.loopin.api.entity.Notification;
import com.loopin.api.entity.NotificationDelivery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class N8nNotificationClient {

    private final N8nNotificationProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void deliver(NotificationDelivery delivery) throws IOException, InterruptedException {
        Notification notification = delivery.getNotification();
        String payload = serializePayload(delivery, notification);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(properties.getWebhookUrl()))
                .timeout(properties.getTimeout())
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", delivery.getPublicId().toString())
                .POST(HttpRequest.BodyPublishers.ofString(payload));

        if (properties.getSecret() != null && !properties.getSecret().isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + properties.getSecret());
        }

        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build()
                .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("n8n returned HTTP " + response.statusCode());
        }
    }

    private String serializePayload(
            NotificationDelivery delivery,
            Notification notification) throws JsonProcessingException {
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("id", notification.getRecipient().getPublicId());
        recipient.put("name", notification.getRecipient().getName());
        recipient.put("email", notification.getRecipient().getEmail());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deliveryId", delivery.getPublicId());
        payload.put("notificationId", notification.getPublicId());
        payload.put("type", notification.getType());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("recipient", recipient);
        payload.put("referenceType", notification.getReferenceType());
        payload.put("referenceId", notification.getReferenceId());
        payload.put("createdAt", notification.getCreatedAt());
        return objectMapper.writeValueAsString(payload);
    }
}
