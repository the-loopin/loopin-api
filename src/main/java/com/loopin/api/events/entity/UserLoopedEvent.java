package com.loopin.api.events.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.users.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "user_looped_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_looped_events_user_event",
        columnNames = {"user_id", "event_id"}
    )
)
@Getter
@NoArgsConstructor
public class UserLoopedEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    public UserLoopedEvent(User user, Event event) {
        this.user = user;
        this.event = event;
    }
}
