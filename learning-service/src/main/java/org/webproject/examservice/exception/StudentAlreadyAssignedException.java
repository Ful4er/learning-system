package org.webproject.examservice.exception;

public class StudentAlreadyAssignedException extends RuntimeException {
    public StudentAlreadyAssignedException(String message) {
        super(message);
    }
    
    public StudentAlreadyAssignedException(Long examId, Long studentId) {
        super("Student " + studentId + " is already assigned to exam " + examId);
    }
}
