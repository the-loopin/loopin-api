package com.loopin.api.service.abstraction;

public interface UserPresenceService {
    void handleUserConnect(Long userId);
    void handleUserDisconnect(Long userId);
}
