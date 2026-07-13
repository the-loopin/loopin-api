package com.loopin.api.recommendation.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class EmbeddingJobOperations {
    private final EmbeddingJobRepository repository;

    @Transactional
    public int retryDeadJobs(Collection<Long> jobIds) {
        return repository.retryDead(jobIds);
    }
}
