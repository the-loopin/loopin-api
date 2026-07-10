package com.loopin.api.moderation.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.users.entity.User;
import com.loopin.api.moderation.enums.ModerationAction;
import com.loopin.api.moderation.enums.ModerationContentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "moderation_logs")
@Getter
@Setter
@NoArgsConstructor
public class ModerationLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ModerationContentType contentType;

    @Column(name = "content_public_id", nullable = false)
    private UUID contentPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModerationAction action;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;
}
