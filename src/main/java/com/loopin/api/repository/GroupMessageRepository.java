package com.loopin.api.repository;

import com.loopin.api.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    List<GroupMessage> findByGroupIdOrderByCreatedAtAsc(Long groupId);

    // Future group auto-archive/delete flow should call this to remove chat history with the group.
    void deleteByGroupId(Long groupId);
}