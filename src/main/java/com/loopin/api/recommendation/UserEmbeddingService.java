package com.loopin.api.recommendation;

import com.loopin.api.entity.Interest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserEmbeddingService {

    private final ApplicationEventPublisher eventPublisher;
    private final UserEmbeddingTextBuilder userEmbeddingTextBuilder;

    public void indexUser(Long userId, List<Interest> interests) {
        String sourceText = userEmbeddingTextBuilder.build(interests);
        if (userId == null || sourceText.isBlank()) {
            return;
        }

        eventPublisher.publishEvent(new UserEmbeddingRequestedEvent(userId, sourceText));
    }
}
