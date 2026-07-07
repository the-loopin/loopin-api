package com.loopin.api.job;

import com.loopin.api.entity.JobLock;
import com.loopin.api.repository.JobLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventCompletionJobLockService {

    private final JobLockRepository jobLockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean acquireLock(String lockName, LocalDateTime now, Duration lockAtMostFor) {
        JobLock lock = jobLockRepository.findByLockNameForUpdate(lockName)
                .orElseThrow(() -> new IllegalStateException("Missing job lock row: " + lockName));

        if (lock.getLockedUntil().isAfter(now)) {
            return false;
        }

        lock.setLockedAt(now);
        lock.setLockedUntil(now.plus(lockAtMostFor));
        jobLockRepository.save(lock);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLock(String lockName, LocalDateTime now) {
        jobLockRepository.findById(lockName).ifPresent(lock -> {
            lock.setLockedUntil(now);
            jobLockRepository.save(lock);
        });
    }
}
