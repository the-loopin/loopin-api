package com.loopin.api.service.abstraction;

import com.loopin.api.dto.auth.request.GoogleLoginRequest;
import com.loopin.api.dto.auth.response.AuthResponse;

public interface AuthService {
    AuthResponse authenticateGoogleUser(GoogleLoginRequest request);
}
