package org.webproject.examservice.exception;

public class InvalidExamStateException extends RuntimeException {
    public InvalidExamStateException(String message) {
        super(message);
    }
    
    public InvalidExamStateException(String currentState, String expectedState) {
        super("Invalid exam state. Current: " + currentState + ", Expected: " + expectedState);
    }
}


