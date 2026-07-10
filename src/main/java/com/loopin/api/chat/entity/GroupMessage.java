package com.loopin.api.chat.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessage extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private EventGroup group;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String messageText;
}
