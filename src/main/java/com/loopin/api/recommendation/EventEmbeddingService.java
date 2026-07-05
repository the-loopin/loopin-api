package com.loopin.api.recommendation;

import com.loopin.api.ai.LoopinAiClient;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.entity.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventEmbeddingService {

    private final LoopinAiClient loopinAiClient;
    private final EventEmbeddingRepository eventEmbeddingRepository;
    private final EventEmbeddingTextBuilder eventEmbeddingTextBuilder;

    public void indexEvent(Event event) {
        String sourceText = eventEmbeddingTextBuilder.build(event);
        if (sourceText.isBlank()) {
            return;
        }

        try {
            EmbeddingResponse response = loopinAiClient.embedPassage(sourceText);
            eventEmbeddingRepository.upsert(
                    event.getId(),
                    response.embedding(),
                    response.model(),
                    sha256(sourceText)
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to index event embedding for event {}", event.getId(), exception);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
