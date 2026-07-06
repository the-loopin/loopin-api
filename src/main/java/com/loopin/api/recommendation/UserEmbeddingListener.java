package com.loopin.api.recommendation;

import com.loopin.api.ai.LoopinAiClient;
import com.loopin.api.ai.dto.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEmbeddingListener {

    private final LoopinAiClient loopinAiClient;
    private final UserEmbeddingRepository userEmbeddingRepository;

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserEmbeddingRequestedEvent event) {
        try {
            EmbeddingResponse response = loopinAiClient.embedPassage(event.sourceText());
            userEmbeddingRepository.upsert(
                    event.userId(),
                    response.embedding(),
                    response.model(),
                    sha256(event.sourceText())
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to index user embedding for user {}", event.userId(), exception);
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
