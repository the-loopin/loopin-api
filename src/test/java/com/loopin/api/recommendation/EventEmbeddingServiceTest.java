package com.loopin.api.recommendation;

import com.loopin.api.ai.client.LoopinAiClient;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.events.entity.Event;
import com.loopin.api.recommendation.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.Mockito.*;

class EventEmbeddingServiceTest {

    private ApplicationEventPublisher eventPublisher;
    private EventEmbeddingTextBuilder eventEmbeddingTextBuilder;
    private EventEmbeddingService eventEmbeddingService;

    private LoopinAiClient loopinAiClient;
    private EventEmbeddingRepository eventEmbeddingRepository;
    private EventEmbeddingListener eventEmbeddingListener;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        eventEmbeddingTextBuilder = mock(EventEmbeddingTextBuilder.class);
        eventEmbeddingService = new EventEmbeddingService(eventPublisher, eventEmbeddingTextBuilder);

        loopinAiClient = mock(LoopinAiClient.class);
        eventEmbeddingRepository = mock(EventEmbeddingRepository.class);
        eventEmbeddingListener = new EventEmbeddingListener(loopinAiClient, eventEmbeddingRepository);
    }

    @Test
    void indexEvent_withNullEventId_doesNotPublishEvent() {
        Event event = new Event();
        event.setId(null);
        when(eventEmbeddingTextBuilder.build(event)).thenReturn("Event Text");

        eventEmbeddingService.indexEvent(event);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void indexEvent_withBlankSourceText_doesNotPublishEvent() {
        Event event = new Event();
        event.setId(1L);
        when(eventEmbeddingTextBuilder.build(event)).thenReturn("   ");

        eventEmbeddingService.indexEvent(event);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void indexEvent_withValidEvent_publishesEvent() {
        Event event = new Event();
        event.setId(1L);
        when(eventEmbeddingTextBuilder.build(event)).thenReturn("Event Text");

        eventEmbeddingService.indexEvent(event);

        verify(eventPublisher).publishEvent(new EventEmbeddingRequestedEvent(1L, "Event Text"));
    }

    @Test
    void handle_successfulEmbedding_upsertsToRepository() {
        EventEmbeddingRequestedEvent event = new EventEmbeddingRequestedEvent(1L, "Event Text");
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        EmbeddingResponse response = new EmbeddingResponse("model-v1", 3, embedding);

        when(loopinAiClient.embedPassage("Event Text")).thenReturn(response);

        eventEmbeddingListener.handle(event);

        verify(eventEmbeddingRepository).upsert(
                eq(1L),
                eq(embedding),
                eq("model-v1"),
                anyString()
        );
    }

    @Test
    void handle_aiClientThrowsException_logsWarningAndDoesNotThrow() {
        EventEmbeddingRequestedEvent event = new EventEmbeddingRequestedEvent(1L, "Event Text");

        when(loopinAiClient.embedPassage("Event Text")).thenThrow(new RuntimeException("AI Service Down"));

        // Should catch exception and not propagate
        eventEmbeddingListener.handle(event);

        verify(eventEmbeddingRepository, never()).upsert(anyLong(), any(), anyString(), anyString());
    }
}
