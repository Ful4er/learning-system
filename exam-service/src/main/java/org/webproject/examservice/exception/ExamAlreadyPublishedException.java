package org.webproject.examservice.exception;

public class ExamAlreadyPublishedException extends RuntimeException {
    public ExamAlreadyPublishedException(String message) {
        super(message);
    }
    
    public ExamAlreadyPublishedException(Long examId) {
        super("Exam with id " + examId + " is already published and cannot be modified");
    }
}


