package com.loopin.api.recommendation.api;

import com.loopin.api.events.entity.Event;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RecommendationIndexerService implements RecommendationIndexer {
    private final EventEmbeddingService embeddingService;
    public void index(Event event) { embeddingService.indexEvent(event); }
}
