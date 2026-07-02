package com.loopin.api.dto.group.response;



import com.loopin.api.common.enums.GroupSizeType;
import com.loopin.api.common.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class GroupResponse {

    private Long id;
    private Long eventId;          // null for independent groups
    private Long adminId;
    private String adminUsername;
    private String title;
    private GroupSizeType groupSize;
    private int maxMembers;
    private GroupStatus status;
    private String groupNote;
    private int memberCount;
    private LocalDateTime createdAt;
}