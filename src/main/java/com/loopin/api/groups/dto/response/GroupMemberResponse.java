package com.loopin.api.core.groups.dto.response;

import com.loopin.api.core.groups.entity.GroupMember;
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
public class GroupMemberResponse {
    private UUID id;
    private UUID groupId;
    private UUID userId;
    private LocalDateTime joinedAt;

    public static GroupMemberResponse from(GroupMember member) {
        return new GroupMemberResponse(
                member.getPublicId(),
                member.getGroup().getPublicId(),
                member.getUser().getPublicId(),
                member.getJoinedAt()
        );
    }
}
