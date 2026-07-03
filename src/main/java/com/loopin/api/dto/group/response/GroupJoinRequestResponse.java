package com.loopin.api.dto.group.response;


import com.loopin.api.common.enums.RequestStatus;
import com.loopin.api.entity.GroupJoinRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinRequestResponse {

    private Long id;
    private Long groupId;
    private Long userId;
    private RequestStatus status;
    private String message;
    private LocalDateTime createdAt;

    public static GroupJoinRequestResponse from(GroupJoinRequest request) {
        return new GroupJoinRequestResponse(
                request.getId(),
                request.getGroup().getId(),
                request.getUser().getId(),
                request.getStatus(),
                request.getMessage(),
                request.getCreatedAt()
        );
    }
}
