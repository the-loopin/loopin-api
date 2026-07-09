package com.loopin.api.moderation.repository;

import com.loopin.api.moderation.entity.ModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {
}
