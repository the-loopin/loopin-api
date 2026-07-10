package com.loopin.api.users.repository;

import com.loopin.api.users.enums.BadgeType;
import com.loopin.api.users.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);
}
