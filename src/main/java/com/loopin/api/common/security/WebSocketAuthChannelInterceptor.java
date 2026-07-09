package com.loopin.api.common.security;

import com.loopin.api.core.events.entity.EventGroup;
import com.loopin.api.core.events.repository.EventGroupRepository;
import com.loopin.api.core.groups.repository.GroupMemberRepository;
import com.loopin.api.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern GROUP_MESSAGES_TOPIC =
            Pattern.compile("^/topic/groups/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/messages$");

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final GroupMemberRepository groupMemberRepository;
    private final EventGroupRepository eventGroupRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateMessageSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing WebSocket authorization token");
        }

        String token = authorizationHeader.substring(7);
        if (!jwtUtils.isTokenValid(token)) {
            throw new AccessDeniedException("Invalid WebSocket authorization token");
        }

        String userEmail = jwtUtils.getUsernameFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        accessor.setUser(authentication);
    }

    private void validateMessageSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = GROUP_MESSAGES_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        UUID groupPublicId = UUID.fromString(matcher.group(1));
        EventGroup group = eventGroupRepository.findByPublicId(groupPublicId)
                .orElseThrow(() -> new AccessDeniedException("Group not found"));
        Long internalGroupId = group.getId();
        Long currentUserId = getCurrentUserId(accessor.getUser());

        if (!groupMemberRepository.existsByGroupIdAndUserId(internalGroupId, currentUserId)) {
            throw new AccessDeniedException("Only group members can subscribe to group messages");
        }
    }

    private Long getCurrentUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser().getId();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
    }
}
