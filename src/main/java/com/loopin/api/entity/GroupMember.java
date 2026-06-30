package com.loopin.api.entity;

import com.loopin.api.entity.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private EventGroup group;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime joinedAt;

    @PrePersist
    protected void onJoin() {
        this.joinedAt = LocalDateTime.now();
    }
}