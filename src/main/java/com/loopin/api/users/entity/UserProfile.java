package com.loopin.api.users.entity;

import com.loopin.api.common.entity.BaseEntity;
import com.loopin.api.media.entity.MediaAsset;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(
        name = "user_id",
        nullable = false,
        unique = true
    )
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "avatar_media_id",
        unique = true
    )
    private MediaAsset avatarMedia;

    private String name;

    private String city;

    private String bio;
}
