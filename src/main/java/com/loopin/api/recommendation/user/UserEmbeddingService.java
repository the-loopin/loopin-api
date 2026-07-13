package com.loopin.api.recommendation.user;

import com.loopin.api.interests.entity.Interest;
import com.loopin.api.recommendation.job.EmbeddingEntityType;
import com.loopin.api.recommendation.job.EmbeddingJobEnqueuer;
import com.loopin.api.recommendation.job.EmbeddingOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserEmbeddingService {

    private final EmbeddingJobEnqueuer jobEnqueuer;
    private final UserEmbeddingTextBuilder userEmbeddingTextBuilder;

    public void indexUser(Long userId, List<Interest> interests) {
        String sourceText = userEmbeddingTextBuilder.build(interests);
        if (userId == null) {
            return;
        }

        EmbeddingOperation operation = sourceText.isBlank() ? EmbeddingOperation.DELETE : EmbeddingOperation.UPSERT;
        jobEnqueuer.enqueue(EmbeddingEntityType.USER_INTEREST, userId, operation, sourceText);
    }
}
