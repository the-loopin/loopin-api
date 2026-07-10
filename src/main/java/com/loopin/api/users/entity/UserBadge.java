package com.loopin.api.users.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.users.enums.BadgeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_badges",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_badges_user_badge_type",
                columnNames = {"user_id", "badge_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBadge extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private BadgeType badgeType;
}
