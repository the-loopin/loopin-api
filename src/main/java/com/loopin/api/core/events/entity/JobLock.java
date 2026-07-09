package com.loopin.api.core.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_locks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobLock {

    @Id
    @Column(name = "lock_name", nullable = false, length = 120)
    private String lockName;

    @Column(name = "locked_until", nullable = false)
    private LocalDateTime lockedUntil;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;
}
