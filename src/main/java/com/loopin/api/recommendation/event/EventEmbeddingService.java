package com.loopin.api.recommendation.event;

import com.loopin.api.events.entity.Event;
import com.loopin.api.recommendation.job.EmbeddingEntityType;
import com.loopin.api.recommendation.job.EmbeddingJobEnqueuer;
import com.loopin.api.recommendation.job.EmbeddingOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventEmbeddingService {

    private final EmbeddingJobEnqueuer jobEnqueuer;
    private final EventEmbeddingTextBuilder eventEmbeddingTextBuilder;

    public void indexEvent(Event event) {
        String sourceText = eventEmbeddingTextBuilder.build(event);
        if (event.getId() == null || sourceText.isBlank()) {
            return;
        }

        jobEnqueuer.enqueue(EmbeddingEntityType.EVENT, event.getId(), EmbeddingOperation.UPSERT, sourceText);
    }
}
