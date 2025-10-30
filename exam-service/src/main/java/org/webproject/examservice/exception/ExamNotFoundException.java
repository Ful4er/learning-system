package org.webproject.examservice.exception;

public class ExamNotFoundException extends RuntimeException {
    public ExamNotFoundException(String message) {
        super(message);
    }
    
    public ExamNotFoundException(Long examId) {
        super("Exam with id " + examId + " not found");
    }
}

