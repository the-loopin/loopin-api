package com.loopin.api.core.users.repository;

import com.loopin.api.core.users.enums.BadgeType;
import com.loopin.api.core.users.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);
}
