package com.loopin.api.auth.service;

public interface GoogleTokenVerifier {

    GoogleTokenClaims verify(String idToken);
}
