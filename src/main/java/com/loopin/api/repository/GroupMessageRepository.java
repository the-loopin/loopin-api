package com.loopin.api.repository;

import com.loopin.api.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    Optional<GroupMessage> findByPublicId(UUID publicId);
}
