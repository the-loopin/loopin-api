package com.loopin.api.recommendation.event;

import com.loopin.api.entity.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventEmbeddingService {

    private final ApplicationEventPublisher eventPublisher;
    private final EventEmbeddingTextBuilder eventEmbeddingTextBuilder;

    public void indexEvent(Event event) {
        String sourceText = eventEmbeddingTextBuilder.build(event);
        if (event.getId() == null || sourceText.isBlank()) {
            return;
        }

        eventPublisher.publishEvent(new EventEmbeddingRequestedEvent(event.getId(), sourceText));
    }
}
