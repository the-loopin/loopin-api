package com.loopin.api.groups.creategroup;

import com.loopin.api.events.api.EventLookup;
import com.loopin.api.events.entity.Event;
import com.loopin.api.groups.dto.request.CreateGroupRequest;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.entity.EventGroup;
import com.loopin.api.groups.entity.GroupMember;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.mapper.GroupMapper;
import com.loopin.api.groups.repository.EventGroupRepository;
import com.loopin.api.groups.repository.GroupMemberRepository;
import com.loopin.api.groups.shared.finder.GroupFinder;
import com.loopin.api.groups.shared.policy.GroupCapacityPolicy;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.moderation.ContentModerationDecision;
import com.loopin.api.moderation.ContentModerationService;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.GROUP_IMAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateGroupHandlerTest {

    private EventGroupRepository groupRepository;
    private GroupMemberRepository memberRepository;
    private EventLookup eventLookup;
    private GroupMapper groupMapper;
    private GroupFinder groupFinder;
    private GroupCapacityPolicy capacityPolicy;
    private ContentModerationService moderationService;
    private MediaAttachmentManager mediaAttachmentManager;

    private CreateGroupHandler handler;

    @BeforeEach
    void setUp() {
        groupRepository =
            mock(EventGroupRepository.class);

        memberRepository =
            mock(GroupMemberRepository.class);

        eventLookup =
            mock(EventLookup.class);

        groupMapper =
            mock(GroupMapper.class);

        groupFinder =
            mock(GroupFinder.class);

        capacityPolicy =
            mock(GroupCapacityPolicy.class);

        moderationService =
            mock(ContentModerationService.class);

        mediaAttachmentManager =
            mock(MediaAttachmentManager.class);

        handler = new CreateGroupHandler(
            groupRepository,
            memberRepository,
            eventLookup,
            groupMapper,
            groupFinder,
            capacityPolicy,
            moderationService,
            mediaAttachmentManager
        );
    }

    @Test
    void handle_validCommand_attachesImageAndCreatesMembership() {
        CreateGroupRequest request =
            validRequest();

        UUID imageMediaId =
            UUID.randomUUID();

        request.setImageMediaId(
            imageMediaId
        );

        User creator = new User(
            "creator@loopin.test",
            "Creator",
            null
        );

        Event event = new Event();

        EventGroup group =
            new EventGroup();

        GroupResponse response =
            mock(GroupResponse.class);

        MediaAsset imageMedia =
            mock(MediaAsset.class);

        when(
            moderationService.moderate(
                request.getTitle(),
                request.getGroupNote()
            )
        ).thenReturn(
            ContentModerationDecision.approved()
        );

        when(
            groupFinder.findCurrentUser(
                "creator@loopin.test"
            )
        ).thenReturn(creator);

        when(
            eventLookup.findActiveByPublicId(
                request.getEventId()
            )
        ).thenReturn(event);

        when(
            mediaAttachmentManager.attach(
                imageMediaId,
                creator,
                GROUP_IMAGE
            )
        ).thenReturn(imageMedia);

        when(
            groupMapper.toEntity(
                request,
                creator,
                event
            )
        ).thenReturn(group);

        when(
            groupRepository.save(group)
        ).thenReturn(group);

        when(
            groupMapper.toGroupResponse(group)
        ).thenReturn(response);

        GroupResponse result = handler.handle(
            new CreateGroupCommand(
                request,
                "creator@loopin.test"
            )
        );

        assertSame(response, result);

        assertSame(
            imageMedia,
            group.getImageMedia()
        );

        verify(mediaAttachmentManager).attach(
            imageMediaId,
            creator,
            GROUP_IMAGE
        );

        verify(capacityPolicy)
            .applyMaximumFromSize(group);

        verify(capacityPolicy)
            .refreshStatus(
                group,
                1
            );

        ArgumentCaptor<GroupMember>
            membershipCaptor =
            ArgumentCaptor.forClass(
                GroupMember.class
            );

        verify(memberRepository).save(
            membershipCaptor.capture()
        );

        GroupMember membership =
            membershipCaptor.getValue();

        assertSame(
            group,
            membership.getGroup()
        );

        assertSame(
            creator,
            membership.getUser()
        );
    }

    @Test
    void handle_withoutImage_createsGroupWithoutImage() {
        CreateGroupRequest request =
            validRequest();

        request.setImageMediaId(null);

        User creator = new User();
        Event event = new Event();
        EventGroup group = new EventGroup();

        when(
            moderationService.moderate(
                request.getTitle(),
                request.getGroupNote()
            )
        ).thenReturn(
            ContentModerationDecision.approved()
        );

        when(
            groupFinder.findCurrentUser(
                "creator@loopin.test"
            )
        ).thenReturn(creator);

        when(
            eventLookup.findActiveByPublicId(
                request.getEventId()
            )
        ).thenReturn(event);

        when(
            mediaAttachmentManager.attach(
                null,
                creator,
                GROUP_IMAGE
            )
        ).thenReturn(null);

        when(
            groupMapper.toEntity(
                request,
                creator,
                event
            )
        ).thenReturn(group);

        when(
            groupRepository.save(group)
        ).thenReturn(group);

        handler.handle(
            new CreateGroupCommand(
                request,
                "creator@loopin.test"
            )
        );

        assertEquals(
            null,
            group.getImageMedia()
        );
    }

    @Test
    void handle_rejectedModeration_stopsBeforeMediaAttachment() {
        CreateGroupRequest request =
            validRequest();

        when(
            moderationService.moderate(
                request.getTitle(),
                request.getGroupNote()
            )
        ).thenReturn(
            new ContentModerationDecision(
                ContentModerationStatus.PENDING_REVIEW,
                List.of("blocked")
            )
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> handler.handle(
                new CreateGroupCommand(
                    request,
                    "creator@loopin.test"
                )
            )
        );

        verifyNoInteractions(
            mediaAttachmentManager,
            groupRepository,
            memberRepository
        );

        verify(
            groupFinder,
            never()
        ).findCurrentUser(
            "creator@loopin.test"
        );
    }

    private CreateGroupRequest validRequest() {
        CreateGroupRequest request =
            new CreateGroupRequest();

        request.setEventId(
            UUID.randomUUID()
        );

        request.setTitle(
            "Loopin Test Group"
        );

        request.setGroupSize(
            GroupSizeType.FOUR_PLUS
        );

        request.setMaxMembers(8);

        request.setGroupNote(
            "Meet near the entrance."
        );

        return request;
    }
}
