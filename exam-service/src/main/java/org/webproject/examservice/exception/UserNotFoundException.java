package org.webproject.examservice.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super("User not found for email: " + email);
    }
}
