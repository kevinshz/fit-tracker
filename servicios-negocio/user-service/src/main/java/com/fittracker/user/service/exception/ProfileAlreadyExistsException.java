package com.fittracker.user.service.exception;

public class ProfileAlreadyExistsException extends RuntimeException {

    public ProfileAlreadyExistsException(String message) {
        super(message);
    }

    public ProfileAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
