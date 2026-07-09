package com.loopin.api.chat.repository;

import com.loopin.api.chat.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    Page<GroupMessage> findByGroupIdOrderByCreatedAtAsc(Long groupId, Pageable pageable);
    
    // Future group auto-archive/delete flow should call this to remove chat history with the group.
    void deleteByGroupId(Long groupId);
    
    Optional<GroupMessage> findByPublicId(UUID publicId);
}
