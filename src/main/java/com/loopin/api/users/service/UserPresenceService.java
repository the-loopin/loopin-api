package com.loopin.api.core.users.service;

public interface UserPresenceService {
    void handleUserConnect(Long userId);
    void handleUserDisconnect(Long userId);
}
