package org.webproject.examservice.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
    
    public AccessDeniedException(Long userId, String resource) {
        super("User " + userId + " does not have access to " + resource);
    }
}

