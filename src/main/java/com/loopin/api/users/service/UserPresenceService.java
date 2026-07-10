package com.loopin.api.users.service;

public interface UserPresenceService {
    void handleUserConnect(Long userId);
    void handleUserDisconnect(Long userId);
}
