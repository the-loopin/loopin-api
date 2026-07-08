package com.loopin.api.common.security;

import com.loopin.api.service.abstraction.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketLifecycleListener {

    private final UserPresenceService userPresenceService;
    private final Map<String, Long> sessionStartTimes = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Long userId = getUserIdFromPrincipal(event.getUser());

        sessionStartTimes.put(sessionId, System.currentTimeMillis());

        log.info("WebSocket connection established [sessionId: {}, userId: {}]", sessionId, userId);

        if (userId != null) {
            userPresenceService.handleUserConnect(userId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long startTime = sessionStartTimes.remove(sessionId);

        // If startTime is null, we've already processed this disconnect (idempotent)
        if (startTime == null) {
            return;
        }

        Long userId = getUserIdFromPrincipal(event.getUser());
        long durationMs = System.currentTimeMillis() - startTime;

        log.info("WebSocket connection closed [sessionId: {}, userId: {}, closeStatus: {}, durationMs: {}]",
                sessionId, userId, event.getCloseStatus(), durationMs);

        if (userId != null) {
            userPresenceService.handleUserDisconnect(userId);
        }
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth &&
            auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }
}
