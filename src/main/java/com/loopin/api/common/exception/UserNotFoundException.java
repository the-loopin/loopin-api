package com.loopin.api.common.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
}
