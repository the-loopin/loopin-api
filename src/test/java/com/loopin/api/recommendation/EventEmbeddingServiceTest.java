package com.loopin.api.recommendation;

import com.loopin.api.events.entity.Event;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.recommendation.event.EventEmbeddingTextBuilder;
import com.loopin.api.recommendation.job.EmbeddingEntityType;
import com.loopin.api.recommendation.job.EmbeddingJobEnqueuer;
import com.loopin.api.recommendation.job.EmbeddingOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class EventEmbeddingServiceTest {
    private final EmbeddingJobEnqueuer enqueuer = mock(EmbeddingJobEnqueuer.class);
    private final EventEmbeddingTextBuilder textBuilder = mock(EventEmbeddingTextBuilder.class);
    private EventEmbeddingService service;

    @BeforeEach void setUp() { service = new EventEmbeddingService(enqueuer, textBuilder); }

    @Test void nullEventIdDoesNotCreateJob() {
        Event event = new Event();
        when(textBuilder.build(event)).thenReturn("Event Text");
        service.indexEvent(event);
        verifyNoInteractions(enqueuer);
    }

    @Test void blankSourceTextDoesNotCreateJob() {
        Event event = new Event(); event.setId(1L);
        when(textBuilder.build(event)).thenReturn("   ");
        service.indexEvent(event);
        verifyNoInteractions(enqueuer);
    }

    @Test void validEventCreatesJobInsideCallingTransaction() {
        Event event = new Event(); event.setId(1L);
        when(textBuilder.build(event)).thenReturn("Event Text");
        service.indexEvent(event);
        verify(enqueuer).enqueue(EmbeddingEntityType.EVENT, 1L, EmbeddingOperation.UPSERT, "Event Text");
    }
}
