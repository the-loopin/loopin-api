package com.loopin.api.events.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.users.entity.User;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventGroup extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    private String title;

    @Enumerated(EnumType.STRING)
    private GroupSizeType groupSize;

    private int maxMembers;

    @Enumerated(EnumType.STRING)
    private GroupStatus status = GroupStatus.OPEN;

    private String groupNote;
}
