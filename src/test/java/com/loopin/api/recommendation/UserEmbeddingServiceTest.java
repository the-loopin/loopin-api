package com.loopin.api.recommendation;

import com.loopin.api.ai.client.LoopinAiClient;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.interests.entity.Interest;
import com.loopin.api.recommendation.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.Mockito.*;

class UserEmbeddingServiceTest {

    private ApplicationEventPublisher eventPublisher;
    private UserEmbeddingTextBuilder userEmbeddingTextBuilder;
    private UserEmbeddingService userEmbeddingService;

    private LoopinAiClient loopinAiClient;
    private UserEmbeddingRepository userEmbeddingRepository;
    private UserEmbeddingListener userEmbeddingListener;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        userEmbeddingTextBuilder = mock(UserEmbeddingTextBuilder.class);
        userEmbeddingService = new UserEmbeddingService(eventPublisher, userEmbeddingTextBuilder);

        loopinAiClient = mock(LoopinAiClient.class);
        userEmbeddingRepository = mock(UserEmbeddingRepository.class);
        userEmbeddingListener = new UserEmbeddingListener(loopinAiClient, userEmbeddingRepository);
    }

    @Test
    void indexUser_withNullUserId_doesNotPublishEvent() {
        List<Interest> interests = List.of(new Interest("Sports", "sports", "category"));
        when(userEmbeddingTextBuilder.build(interests)).thenReturn("interest: Sports");

        userEmbeddingService.indexUser(null, interests);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void indexUser_withBlankSourceText_doesNotPublishEvent() {
        List<Interest> interests = List.of();
        when(userEmbeddingTextBuilder.build(interests)).thenReturn("   ");

        userEmbeddingService.indexUser(1L, interests);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void indexUser_withValidUser_publishesEvent() {
        List<Interest> interests = List.of(new Interest("Sports", "sports", "category"));
        when(userEmbeddingTextBuilder.build(interests)).thenReturn("interest: Sports");

        userEmbeddingService.indexUser(1L, interests);

        verify(eventPublisher).publishEvent(new UserEmbeddingRequestedEvent(1L, "interest: Sports"));
    }

    @Test
    void handle_successfulEmbedding_upsertsToRepository() {
        UserEmbeddingRequestedEvent event = new UserEmbeddingRequestedEvent(1L, "interest: Sports");
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        EmbeddingResponse response = new EmbeddingResponse("model-v1", 3, embedding);

        when(loopinAiClient.embedPassage("interest: Sports")).thenReturn(response);

        userEmbeddingListener.handle(event);

        verify(userEmbeddingRepository).upsert(
                eq(1L),
                eq(embedding),
                eq("model-v1"),
                anyString()
        );
    }

    @Test
    void handle_aiClientThrowsException_logsWarningAndDoesNotThrow() {
        UserEmbeddingRequestedEvent event = new UserEmbeddingRequestedEvent(1L, "interest: Sports");

        when(loopinAiClient.embedPassage("interest: Sports")).thenThrow(new RuntimeException("AI Service Down"));

        // Should catch exception and not propagate
        userEmbeddingListener.handle(event);

        verify(userEmbeddingRepository, never()).upsert(anyLong(), any(), anyString(), anyString());
    }
}
