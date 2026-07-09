package com.loopin.api.core.groups.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.core.events.entity.EventGroup;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.core.groups.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_join_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinRequest extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private EventGroup group;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(length = 500)
    private String message;
}
