package com.loopin.api.core.interests.entity;

import com.loopin.api.core.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_interests")
@Getter
@Setter
@NoArgsConstructor
public class UserInterest {

    @EmbeddedId
    private UserInterestId id = new UserInterestId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("interestId")
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight = BigDecimal.ONE;

    @Column(nullable = false, length = 50)
    private String source = "USER";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UserInterest(User user, Interest interest, BigDecimal weight, String source) {
        this.user = user;
        this.interest = interest;
        this.id = new UserInterestId(
                user == null ? null : user.getId(),
                interest == null ? null : interest.getId()
        );
        this.weight = weight == null ? BigDecimal.ONE : weight;
        this.source = source == null || source.isBlank() ? "USER" : source;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
