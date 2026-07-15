package com.loopin.api.common.security;

import com.loopin.api.auth.service.CustomUserDetailsService;
import com.loopin.api.groups.api.GroupSubscriptionAuthorization;
import com.loopin.api.groups.api.GroupSubscriptionAuthorizer;
import com.loopin.api.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketAuthChannelInterceptorTest {

    private static final UUID GROUP_PUBLIC_ID =
            UUID.fromString(
                    "76683fa8-1d84-4ea0-a3d4-cced3e2d43e9"
            );

    private static final Long USER_ID = 42L;

    private GroupSubscriptionAuthorizer groupSubscriptionAuthorizer;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        groupSubscriptionAuthorizer =
                mock(GroupSubscriptionAuthorizer.class);

        interceptor =
                new WebSocketAuthChannelInterceptor(
                        mock(JwtUtils.class),
                        mock(CustomUserDetailsService.class),
                        groupSubscriptionAuthorizer
                );
    }

    @Test
    void activeMemberCanSubscribe() {
        allowGroupAccess();

        assertDoesNotThrow(() ->
                interceptor.preSend(
                        authenticatedMessage(
                                StompCommand.SUBSCRIBE,
                                groupTopic()
                        ),
                        mock(MessageChannel.class)
                )
        );

        verify(groupSubscriptionAuthorizer)
                .authorize(GROUP_PUBLIC_ID, USER_ID);
    }

    @Test
    void nonMemberCannotSubscribe() {
        when(groupSubscriptionAuthorizer.authorize(
                GROUP_PUBLIC_ID,
                USER_ID
        )).thenReturn(
                GroupSubscriptionAuthorization.NOT_A_MEMBER
        );

        AccessDeniedException exception =
                assertThrows(
                        AccessDeniedException.class,
                        () -> interceptor.preSend(
                                authenticatedMessage(
                                        StompCommand.SUBSCRIBE,
                                        groupTopic()
                                ),
                                mock(MessageChannel.class)
                        )
                );

        assertEquals(
                "Only group members can subscribe to group messages",
                exception.getMessage()
        );
    }

    @Test
    void missingGroupCannotBeSubscribedTo() {
        when(groupSubscriptionAuthorizer.authorize(
                GROUP_PUBLIC_ID,
                USER_ID
        )).thenReturn(
                GroupSubscriptionAuthorization.GROUP_NOT_FOUND
        );

        AccessDeniedException exception =
                assertThrows(
                        AccessDeniedException.class,
                        () -> interceptor.preSend(
                                authenticatedMessage(
                                        StompCommand.SUBSCRIBE,
                                        groupTopic()
                                ),
                                mock(MessageChannel.class)
                        )
                );

        assertEquals(
                "Group not found",
                exception.getMessage()
        );
    }

    @Test
    void arbitrarySubscriptionDestinationIsRejected() {
        AccessDeniedException exception =
                assertThrows(
                        AccessDeniedException.class,
                        () -> interceptor.preSend(
                                authenticatedMessage(
                                        StompCommand.SUBSCRIBE,
                                        "/topic/admin"
                                ),
                                mock(MessageChannel.class)
                        )
                );

        assertEquals(
                "Unsupported subscription destination",
                exception.getMessage()
        );

        verify(
                groupSubscriptionAuthorizer,
                never()
        ).authorize(
                GROUP_PUBLIC_ID,
                USER_ID
        );
    }

    @Test
    void directBrokerPublishingIsRejected() {
        AccessDeniedException exception =
                assertThrows(
                        AccessDeniedException.class,
                        () -> interceptor.preSend(
                                authenticatedMessage(
                                        StompCommand.SEND,
                                        groupTopic()
                                ),
                                mock(MessageChannel.class)
                        )
                );

        assertEquals(
                "Direct broker publishing is not allowed",
                exception.getMessage()
        );

        verify(
                groupSubscriptionAuthorizer,
                never()
        ).authorize(
                GROUP_PUBLIC_ID,
                USER_ID
        );
    }

    @Test
    void activeMemberCanSendThroughApplicationDestination() {
        allowGroupAccess();

        assertDoesNotThrow(() ->
                interceptor.preSend(
                        authenticatedMessage(
                                StompCommand.SEND,
                                groupApplicationDestination()
                        ),
                        mock(MessageChannel.class)
                )
        );

        verify(groupSubscriptionAuthorizer)
                .authorize(GROUP_PUBLIC_ID, USER_ID);
    }

    @Test
    void nonMemberCannotSendThroughApplicationDestination() {
        when(groupSubscriptionAuthorizer.authorize(
                GROUP_PUBLIC_ID,
                USER_ID
        )).thenReturn(
                GroupSubscriptionAuthorization.NOT_A_MEMBER
        );

        AccessDeniedException exception =
                assertThrows(
                        AccessDeniedException.class,
                        () -> interceptor.preSend(
                                authenticatedMessage(
                                        StompCommand.SEND,
                                        groupApplicationDestination()
                                ),
                                mock(MessageChannel.class)
                        )
                );

        assertEquals(
                "Only group members can send group messages",
                exception.getMessage()
        );
    }

    @Test
    void arbitraryApplicationDestinationIsRejected() {
        AccessDeniedException exception =
                assertThrows(
                        AccessDeniedException.class,
                        () -> interceptor.preSend(
                                authenticatedMessage(
                                        StompCommand.SEND,
                                        "/app/admin/broadcast"
                                ),
                                mock(MessageChannel.class)
                        )
                );

        assertEquals(
                "Unsupported message destination",
                exception.getMessage()
        );
    }

    @Test
    void unauthenticatedPrincipalIsRejected() {
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> interceptor.preSend(
                                messageWithoutPrincipal(
                                        StompCommand.SUBSCRIBE,
                                        groupTopic()
                                ),
                                mock(MessageChannel.class)
                        )
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    private void allowGroupAccess() {
        when(groupSubscriptionAuthorizer.authorize(
                GROUP_PUBLIC_ID,
                USER_ID
        )).thenReturn(
                GroupSubscriptionAuthorization.ALLOWED
        );
    }

    private Message<byte[]> authenticatedMessage(
            StompCommand command,
            String destination
    ) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.create(command);

        accessor.setDestination(destination);

        User user = new User(
                "member@example.com",
                "Member",
                null
        );

        user.setId(USER_ID);
        user.setIsActive(true);

        CustomUserDetails userDetails =
                new CustomUserDetails(user);

        accessor.setUser(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );

        return MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );
    }

    private Message<byte[]> messageWithoutPrincipal(
            StompCommand command,
            String destination
    ) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.create(command);

        accessor.setDestination(destination);

        return MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );
    }

    private String groupTopic() {
        return "/topic/groups/"
                + GROUP_PUBLIC_ID
                + "/messages";
    }

    private String groupApplicationDestination() {
        return "/app/groups/"
                + GROUP_PUBLIC_ID
                + "/messages";
    }
}
