package com.loopin.api.recommendation;

import com.loopin.api.interests.entity.Interest;
import com.loopin.api.recommendation.job.EmbeddingEntityType;
import com.loopin.api.recommendation.job.EmbeddingJobEnqueuer;
import com.loopin.api.recommendation.job.EmbeddingOperation;
import com.loopin.api.recommendation.user.UserEmbeddingService;
import com.loopin.api.recommendation.user.UserEmbeddingTextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class UserEmbeddingServiceTest {
    private final EmbeddingJobEnqueuer enqueuer = mock(EmbeddingJobEnqueuer.class);
    private final UserEmbeddingTextBuilder textBuilder = mock(UserEmbeddingTextBuilder.class);
    private UserEmbeddingService service;

    @BeforeEach void setUp() { service = new UserEmbeddingService(enqueuer, textBuilder); }

    @Test void nullUserIdDoesNotCreateJob() {
        List<Interest> interests = List.of(new Interest("Sports", "sports", "category"));
        when(textBuilder.build(interests)).thenReturn("interest: Sports");
        service.indexUser(null, interests);
        verifyNoInteractions(enqueuer);
    }

    @Test void emptyInterestSetCreatesDeleteJob() {
        when(textBuilder.build(List.of())).thenReturn("");
        service.indexUser(1L, List.of());
        verify(enqueuer).enqueue(EmbeddingEntityType.USER_INTEREST, 1L, EmbeddingOperation.DELETE, "");
    }

    @Test void interestsCreateUpsertJob() {
        List<Interest> interests = List.of(new Interest("Sports", "sports", "category"));
        when(textBuilder.build(interests)).thenReturn("interest: Sports");
        service.indexUser(1L, interests);
        verify(enqueuer).enqueue(EmbeddingEntityType.USER_INTEREST, 1L,
                EmbeddingOperation.UPSERT, "interest: Sports");
    }
}
