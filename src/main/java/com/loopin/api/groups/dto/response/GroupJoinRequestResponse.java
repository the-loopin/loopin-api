package com.loopin.api.groups.dto.response;


import com.loopin.api.groups.enums.RequestStatus;
import com.loopin.api.groups.entity.GroupJoinRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupJoinRequestResponse {

    private UUID id;
    private UUID groupId;
    private UUID userId;
    private RequestStatus status;
    private String message;
    private LocalDateTime createdAt;

    public static GroupJoinRequestResponse from(GroupJoinRequest request) {
        return new GroupJoinRequestResponse(
                request.getPublicId(),
                request.getGroup().getPublicId(),
                request.getUser().getPublicId(),
                request.getStatus(),
                request.getMessage(),
                request.getCreatedAt()
        );
    }
}
