package org.webproject.examservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webproject.examservice.dto.request.StartExamAttemptRequest;
import org.webproject.examservice.model.StudentAnswer;
import org.webproject.examservice.exception.ExamNotFoundException;
import org.webproject.examservice.exception.InvalidExamStateException;
import org.webproject.examservice.model.Exam;
import org.webproject.examservice.model.ExamAssignment;
import org.webproject.examservice.model.ExamAttempt;
import org.webproject.examservice.repository.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentExamServiceImplTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamAssignmentRepository examAssignmentRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionOptionRepository questionOptionRepository;
    @Mock private ExamAttemptRepository examAttemptRepository;
    @Mock private StudentAnswerRepository studentAnswerRepository;

    @InjectMocks private StudentExamServiceImpl service;

    @Test
    void startExamAttempt_happyPath_createsAttempt() {
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(7L);

        Exam exam = new Exam();
        exam.setId(7L);
        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        when(examRepository.findById(7L)).thenReturn(Optional.of(exam));

        when(examAssignmentRepository.findByExamIdAndStudentId(7L, 100L)).thenReturn(Optional.of(new ExamAssignment()));
        when(examAttemptRepository.findAllByExamIdAndStudentId(7L, 100L)).thenReturn(List.of());

        ExamAttempt saved = new ExamAttempt();
        saved.setId(55L);
        saved.setExamId(7L);
        saved.setStudentId(100L);
        saved.setStatus(ExamAttempt.AttemptStatus.IN_PROGRESS);
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenReturn(saved);

        var response = service.startExamAttempt(100L, req);
        assertEquals(55L, response.getId());
        assertEquals("IN_PROGRESS", response.getStatus());
    }

    @Test
    void startExamAttempt_examNotPublished_throws() {
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(7L);
        Exam exam = new Exam();
        exam.setId(7L);
        exam.setStatus(Exam.ExamStatus.DRAFT);
        when(examRepository.findById(7L)).thenReturn(Optional.of(exam));
        assertThrows(InvalidExamStateException.class, () -> service.startExamAttempt(100L, req));
    }

    @Test
    void startExamAttempt_examMissing_throws() {
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(7L);
        when(examRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(ExamNotFoundException.class, () -> service.startExamAttempt(100L, req));
    }

    @Test
    void startExamAttempt_activeAttemptExists_throws() {
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(7L);
        Exam exam = new Exam();
        exam.setId(7L);
        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        when(examRepository.findById(7L)).thenReturn(Optional.of(exam));
        when(examAssignmentRepository.findByExamIdAndStudentId(7L, 100L)).thenReturn(Optional.of(new ExamAssignment()));

        ExamAttempt existing = new ExamAttempt();
        existing.setStatus(ExamAttempt.AttemptStatus.IN_PROGRESS);
        when(examAttemptRepository.findAllByExamIdAndStudentId(7L, 100L)).thenReturn(List.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.startExamAttempt(100L, req));
    }

    @Test
    void finishExamAttempt_setsFinishedAndCalculatesScore() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(200L);
        attempt.setExamId(9L);
        attempt.setStudentId(100L);
        attempt.setStatus(ExamAttempt.AttemptStatus.IN_PROGRESS);

        when(examAttemptRepository.findById(200L)).thenReturn(Optional.of(attempt));

        StudentAnswer ans = new StudentAnswer();
        ans.setAttemptId(200L);
        ans.setQuestionId(300L);
        ans.setTextAnswer("some text");
        when(studentAnswerRepository.findAllByAttemptId(200L)).thenReturn(List.of(ans));

        var question = new org.webproject.examservice.model.Question();
        question.setId(300L);
        question.setExamId(9L);
        question.setType(org.webproject.examservice.model.Question.QuestionType.TEXT);
        question.setPoints(10);
        when(questionRepository.findById(300L)).thenReturn(Optional.of(question));

        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.finishExamAttempt(100L, 200L);
        assertEquals("FINISHED", response.getStatus());
        assertNotNull(response.getFinishedAt());
        assertEquals(50.0, response.getCalculatedScore());
    }
}


