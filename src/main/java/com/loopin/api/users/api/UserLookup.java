package com.loopin.api.users.api;

import com.loopin.api.users.entity.User;

import java.util.UUID;

public interface UserLookup {
    User findByEmail(String email);
    User findActiveByPublicId(UUID publicId);
    User findById(Long id);
}
