package com.loopin.api.dto.group.response;

import com.loopin.api.entity.GroupMember;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    private Long id;
    private Long groupId;
    private Long userId;
    private LocalDateTime joinedAt;

    public static GroupMemberResponse from(GroupMember member) {
        return new GroupMemberResponse(
                member.getId(),
                member.getGroup().getId(),
                member.getUser().getId(),
                member.getJoinedAt()
        );
    }
}
