package com.loopin.api.groups.dto.response;

import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.media.dto.response.MediaReferenceResponse;
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

    private MediaReferenceResponse image;

    private int memberCount;

    private LocalDateTime createdAt;

    /**
     * Preserves source compatibility with callers that do not provide
     * group image data yet.
     */
    public GroupResponse(
        UUID id,
        UUID eventId,
        UUID adminId,
        String adminUsername,
        String title,
        GroupSizeType groupSize,
        int maxMembers,
        GroupStatus status,
        String groupNote,
        int memberCount,
        LocalDateTime createdAt
    ) {
        this(
            id,
            eventId,
            adminId,
            adminUsername,
            title,
            groupSize,
            maxMembers,
            status,
            groupNote,
            null,
            memberCount,
            createdAt
        );
    }
}
