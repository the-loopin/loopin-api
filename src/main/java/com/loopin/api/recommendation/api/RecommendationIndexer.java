package com.loopin.api.recommendation.api;

import com.loopin.api.events.entity.Event;

public interface RecommendationIndexer {
    void index(Event event);
}
