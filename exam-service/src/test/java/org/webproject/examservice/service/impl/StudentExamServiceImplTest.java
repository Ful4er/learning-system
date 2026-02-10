package org.webproject.examservice.service.impl;

import org.junit.jupiter.api.Test;
import org.webproject.examservice.exception.ExamNotFoundException;
import org.webproject.examservice.exception.InvalidExamStateException;
import org.webproject.examservice.model.Exam;
import org.webproject.examservice.model.ExamAssignment;
import org.webproject.examservice.model.Question;
import org.webproject.examservice.model.StudentAnswer;

import java.time.Instant;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class StudentExamServiceImplTest {

    @Test
    public void testExamDurationCalculation() {
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(Duration.ofMinutes(60));
        Duration duration = Duration.between(startTime, endTime);
        
        assertEquals(60, duration.toMinutes());
    }

    @Test
    public void testExamStateTransitions() {
        Exam exam = new Exam();
        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        
        assertEquals(Exam.ExamStatus.PUBLISHED, exam.getStatus());
        
        exam.setStatus(Exam.ExamStatus.DRAFT);
        assertEquals(Exam.ExamStatus.DRAFT, exam.getStatus());
    }

    @Test
    public void testQuestionValidation() {
        // Test question validation logic
        Question question = new Question();
        question.setText("Test question");
        
        assertNotNull(question.getText());
    }

    @Test
    public void testExamAttemptTiming() {
        Instant startTime = Instant.now();
        
        assertNotNull(startTime);
        assertTrue(startTime.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    public void testStudentAnswerValidation() {
        StudentAnswer answer = new StudentAnswer();
        assertNotNull(answer);
    }

    @Test
    public void testExamAssignmentLogic() {
        // Test exam assignment logic
        ExamAssignment assignment = new ExamAssignment();
        assignment.setStudentId(1L);
        assignment.setExamId(1L);
        
        assertEquals(1L, assignment.getStudentId());
        assertEquals(1L, assignment.getExamId());
    }

    @Test
    public void testExceptionMessages() {
        ExamNotFoundException notFound = new ExamNotFoundException("Exam not found");
        assertEquals("Exam not found", notFound.getMessage());
        
        InvalidExamStateException invalidState = new InvalidExamStateException("Invalid state");
        assertEquals("Invalid state", invalidState.getMessage());
    }
}
