package com.loopin.api.core.groups.dto.response;



import com.loopin.api.core.groups.enums.GroupSizeType;
import com.loopin.api.core.groups.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class GroupResponse {

    private UUID id;
    private UUID eventId;
    private UUID adminId;
    private String adminUsername;
    private String title;
    private GroupSizeType groupSize;
    private int maxMembers;
    private GroupStatus status;
    private String groupNote;
    private int memberCount;
    private LocalDateTime createdAt;
}
