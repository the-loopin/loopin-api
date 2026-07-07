package com.loopin.api.repository;

import com.loopin.api.common.enums.BadgeType;
import com.loopin.api.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    boolean existsByUserIdAndBadgeType(Long userId, BadgeType badgeType);
}
