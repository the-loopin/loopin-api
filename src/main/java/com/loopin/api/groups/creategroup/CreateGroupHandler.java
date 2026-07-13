package com.loopin.api.groups.creategroup;

import com.loopin.api.common.metrics.LoopinOperation;
import com.loopin.api.events.api.EventLookup;
import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.moderation.ContentModerationService;
import com.loopin.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.loopin.api.media.enums.MediaPurpose.GROUP_IMAGE;

@Component
@RequiredArgsConstructor
public class CreateGroupHandler {

    private final EventGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final EventLookup eventLookup;
    private final GroupMapper groupMapper;
    private final GroupFinder groupFinder;
    private final GroupCapacityPolicy capacityPolicy;
    private final ContentModerationService moderationService;
    private final MediaAttachmentManager mediaAttachmentManager;

    @Transactional
    @LoopinOperation(
        domain = "groups",
        operation = "create"
    )
    public GroupResponse handle(
        CreateGroupCommand command
    ) {
        var request = command.request();

        if (!moderationService
            .moderate(
                request.getTitle(),
                request.getGroupNote()
            )
            .isApproved()) {
            throw new IllegalArgumentException(
                "Content contains blocked language "
                    + "and cannot be published"
            );
        }

        User creator = groupFinder.findCurrentUser(
            command.currentUsername()
        );

        Event event = eventLookup.findActiveByPublicId(
            request.getEventId()
        );

        MediaAsset imageMedia =
            mediaAttachmentManager.attach(
                request.getImageMediaId(),
                creator,
                GROUP_IMAGE
            );

        EventGroup group = groupMapper.toEntity(
            request,
            creator,
            event
        );

        group.setImageMedia(imageMedia);

        capacityPolicy.applyMaximumFromSize(group);

        EventGroup savedGroup =
            groupRepository.save(group);

        GroupMember creatorMembership =
            new GroupMember();

        creatorMembership.setGroup(savedGroup);
        creatorMembership.setUser(creator);

        memberRepository.save(creatorMembership);

        capacityPolicy.refreshStatus(
            savedGroup,
            1
        );

        return groupMapper.toGroupResponse(
            savedGroup
        );
    }
}
