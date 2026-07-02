package com.loopin.api.auth.service;

import com.loopin.api.auth.dto.GoogleLoginRequest;
import com.loopin.api.auth.dto.AuthResponse;

public interface AuthService {
    AuthResponse authenticateGoogleUser(GoogleLoginRequest request);
}
