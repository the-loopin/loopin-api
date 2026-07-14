package com.loopin.api.events.delete;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.repository.EventInterestRepository;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.events.shared.access.EventAccessPolicy;
import com.loopin.api.events.shared.finder.EventFinder;
import com.loopin.api.events.shared.notification.EventMemberNotifier;
import com.loopin.api.groups.api.GroupLifecycle;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.EVENT_IMAGE;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteEventHandlerTest {

    @Test
    void handle_softDeletesEventAndRemovesImage() {
        EventRepository repository =
            mock(EventRepository.class);

        EventInterestRepository interestRepository =
            mock(EventInterestRepository.class);

        GroupLifecycle groupLifecycle =
            mock(GroupLifecycle.class);

        EventFinder finder =
            mock(EventFinder.class);

        EventAccessPolicy accessPolicy =
            mock(EventAccessPolicy.class);

        EventMemberNotifier memberNotifier =
            mock(EventMemberNotifier.class);

        MediaAttachmentManager mediaAttachmentManager =
            mock(MediaAttachmentManager.class);

        DeleteEventHandler handler =
            new DeleteEventHandler(
                repository,
                interestRepository,
                groupLifecycle,
                finder,
                accessPolicy,
                memberNotifier,
                mediaAttachmentManager
            );

        UUID eventId = UUID.randomUUID();

        Event event = new Event();

        event.setId(7L);
        event.setPublicId(eventId);
        event.setTitle("Event");

        MediaAsset imageMedia =
            mock(MediaAsset.class);

        event.setImageMedia(imageMedia);

        User user =
            new User(
                "owner@loopin.test",
                "Owner",
                null
            );

        when(
            finder.findCurrentUser(
                "owner@loopin.test"
            )
        ).thenReturn(user);

        when(
            finder.findActiveEventById(
                eventId
            )
        ).thenReturn(event);

        handler.handle(
            new DeleteEventCommand(
                eventId,
                "owner@loopin.test"
            )
        );

        assertTrue(event.isDeleted());
        assertNull(event.getImageMedia());

        verify(accessPolicy)
            .requireOwnerOrAdmin(
                event,
                user
            );

        verify(memberNotifier)
            .notifyMembers(
                event,
                "Event deleted",
                "\"Event\" has been deleted."
            );

        verify(mediaAttachmentManager)
            .remove(
                imageMedia,
                EVENT_IMAGE
            );

        verify(interestRepository)
            .deleteByEvent_Id(7L);

        verify(groupLifecycle)
            .archiveActiveGroupsForEvent(7L);

        verify(repository)
            .save(event);
    }
}
