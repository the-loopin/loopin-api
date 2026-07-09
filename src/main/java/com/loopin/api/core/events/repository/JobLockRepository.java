package com.loopin.api.core.events.repository;

import com.loopin.api.core.events.entity.JobLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobLockRepository extends JpaRepository<JobLock, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lock from JobLock lock where lock.lockName = :lockName")
    Optional<JobLock> findByLockNameForUpdate(@Param("lockName") String lockName);
}
