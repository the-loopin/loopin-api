package com.loopin.api.entity;

import com.loopin.api.entity.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile profile;
}