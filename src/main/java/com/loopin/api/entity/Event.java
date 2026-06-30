package com.loopin.api.entity;

import com.loopin.api.entity.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event extends BaseEntity {

    private String title;
    private String description;
    private String location;
    private LocalDateTime endDateTime;
    private boolean isCompleted = false;
}
