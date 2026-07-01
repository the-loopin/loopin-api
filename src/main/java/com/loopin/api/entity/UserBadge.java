package com.loopin.api.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.common.enums.BadgeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_badges")
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