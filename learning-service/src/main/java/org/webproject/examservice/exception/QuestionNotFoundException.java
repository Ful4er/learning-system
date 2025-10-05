package org.webproject.examservice.exception;

public class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException(String message) {
        super(message);
    }
    
    public QuestionNotFoundException(Long questionId) {
        super("Question with id " + questionId + " not found");
    }
}
