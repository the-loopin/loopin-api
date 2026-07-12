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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketAuthChannelInterceptorTest {

    private static final UUID GROUP_PUBLIC_ID = UUID.fromString("76683fa8-1d84-4ea0-a3d4-cced3e2d43e9");

    private GroupSubscriptionAuthorizer groupSubscriptionAuthorizer;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        groupSubscriptionAuthorizer = mock(GroupSubscriptionAuthorizer.class);
        interceptor = new WebSocketAuthChannelInterceptor(
                mock(JwtUtils.class),
                mock(CustomUserDetailsService.class),
                groupSubscriptionAuthorizer);
    }

    @Test
    void activeMemberCanSubscribe() {
        when(groupSubscriptionAuthorizer.authorize(GROUP_PUBLIC_ID, 42L))
                .thenReturn(GroupSubscriptionAuthorization.ALLOWED);

        assertDoesNotThrow(() -> interceptor.preSend(subscriptionFor(GROUP_PUBLIC_ID, 42L), mock(MessageChannel.class)));

        verify(groupSubscriptionAuthorizer).authorize(GROUP_PUBLIC_ID, 42L);
    }

    @Test
    void nonMemberCannotSubscribe() {
        when(groupSubscriptionAuthorizer.authorize(GROUP_PUBLIC_ID, 42L))
                .thenReturn(GroupSubscriptionAuthorization.NOT_A_MEMBER);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(subscriptionFor(GROUP_PUBLIC_ID, 42L), mock(MessageChannel.class)));

        assertEquals("Only group members can subscribe to group messages", exception.getMessage());
    }

    @Test
    void missingGroupCannotBeSubscribedTo() {
        when(groupSubscriptionAuthorizer.authorize(GROUP_PUBLIC_ID, 42L))
                .thenReturn(GroupSubscriptionAuthorization.GROUP_NOT_FOUND);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(subscriptionFor(GROUP_PUBLIC_ID, 42L), mock(MessageChannel.class)));

        assertEquals("Group not found", exception.getMessage());
    }

    @Test
    void unauthenticatedPrincipalIsRejected() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> interceptor.preSend(subscriptionWithoutPrincipal(GROUP_PUBLIC_ID), mock(MessageChannel.class)));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    private Message<byte[]> subscriptionFor(UUID groupPublicId, Long userId) {
        StompHeaderAccessor accessor = subscriptionAccessor(groupPublicId);
        User user = new User("member@example.com", "Member", null);
        user.setId(userId);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        accessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscriptionWithoutPrincipal(UUID groupPublicId) {
        StompHeaderAccessor accessor = subscriptionAccessor(groupPublicId);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private StompHeaderAccessor subscriptionAccessor(UUID groupPublicId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/groups/" + groupPublicId + "/messages");
        return accessor;
    }
}
