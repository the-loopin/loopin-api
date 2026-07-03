package com.loopin.api.dto.group.response;


import com.loopin.api.common.enums.RequestStatus;
import com.loopin.api.entity.GroupJoinRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public static GroupJoinRequestResponse from(GroupJoinRequest request) {
        return new GroupJoinRequestResponse(
                request.getId(),
                request.getGroup().getId(),
                request.getUser().getId(),
                request.getStatus(),
                request.getMessage()
        );
    }
}
