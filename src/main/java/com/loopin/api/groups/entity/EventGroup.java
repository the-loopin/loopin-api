package com.loopin.api.groups.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.users.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
