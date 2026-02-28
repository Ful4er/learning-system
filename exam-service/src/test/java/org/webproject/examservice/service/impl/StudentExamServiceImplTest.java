package org.webproject.examservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webproject.examservice.dto.request.StartExamAttemptRequest;
import org.webproject.examservice.dto.response.ExamAttemptResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.exception.ExamNotFoundException;
import org.webproject.examservice.exception.InvalidExamStateException;
import org.webproject.examservice.model.Exam;
import org.webproject.examservice.model.ExamAssignment;
import org.webproject.examservice.model.ExamAttempt;
import org.webproject.examservice.repository.ExamAssignmentRepository;
import org.webproject.examservice.repository.ExamAttemptRepository;
import org.webproject.examservice.repository.ExamRepository;
import org.webproject.examservice.repository.QuestionOptionRepository;
import org.webproject.examservice.repository.QuestionRepository;
import org.webproject.examservice.repository.StudentAnswerRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamAssignmentRepository examAssignmentRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionOptionRepository questionOptionRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private StudentAnswerRepository studentAnswerRepository;

    private StudentExamServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentExamServiceImpl(
                examRepository,
                examAssignmentRepository,
                questionRepository,
                questionOptionRepository,
                examAttemptRepository,
                studentAnswerRepository
        );
    }

    @Test
    void getAssignedExamsForStudent_returnsOnlyPublished() {
        Long studentId = 1L;

        ExamAssignment a1 = new ExamAssignment();
        a1.setExamId(10L);
        a1.setStudentId(studentId);
        ExamAssignment a2 = new ExamAssignment();
        a2.setExamId(20L);
        a2.setStudentId(studentId);

        Exam published = new Exam();
        published.setId(10L);
        published.setStatus(Exam.ExamStatus.PUBLISHED);

        Exam draft = new Exam();
        draft.setId(20L);
        draft.setStatus(Exam.ExamStatus.DRAFT);

        when(examAssignmentRepository.findAllByStudentId(studentId)).thenReturn(List.of(a1, a2));
        when(examRepository.findAllById(eq(List.of(10L, 20L)))).thenReturn(List.of(published, draft));
        when(questionRepository.findAllByExamIdOrderByIdAsc(any())).thenReturn(List.of());
        when(examAssignmentRepository.findAllByExamId(any())).thenReturn(List.of());

        List<ExamResponse> result = service.getAssignedExamsForStudent(studentId);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("PUBLISHED", result.get(0).getStatus());
    }

    @Test
    void startExamAttempt_whenExamNotFound_throws() {
        Long studentId = 1L;
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(99L);

        when(examRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ExamNotFoundException.class, () -> service.startExamAttempt(studentId, req));
    }

    @Test
    void startExamAttempt_whenExamNotPublished_throws() {
        Long studentId = 1L;
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(10L);

        Exam exam = new Exam();
        exam.setId(10L);
        exam.setStatus(Exam.ExamStatus.DRAFT);

        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));

        assertThrows(InvalidExamStateException.class, () -> service.startExamAttempt(studentId, req));
    }

    @Test
    void startExamAttempt_whenNotAssigned_throws() {
        Long studentId = 1L;
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(10L);

        Exam exam = new Exam();
        exam.setId(10L);
        exam.setStatus(Exam.ExamStatus.PUBLISHED);

        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(examAssignmentRepository.findByExamIdAndStudentId(10L, studentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.startExamAttempt(studentId, req));
    }

    @Test
    void startExamAttempt_whenActiveAttemptExists_throws() {
        Long studentId = 1L;
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(10L);

        Exam exam = new Exam();
        exam.setId(10L);
        exam.setStatus(Exam.ExamStatus.PUBLISHED);

        ExamAssignment assignment = new ExamAssignment();
        assignment.setExamId(10L);
        assignment.setStudentId(studentId);

        ExamAttempt active = new ExamAttempt();
        active.setExamId(10L);
        active.setStudentId(studentId);
        active.setStatus(ExamAttempt.AttemptStatus.IN_PROGRESS);

        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(examAssignmentRepository.findByExamIdAndStudentId(10L, studentId)).thenReturn(Optional.of(assignment));
        when(examAttemptRepository.findAllByExamIdAndStudentId(10L, studentId)).thenReturn(List.of(active));

        assertThrows(IllegalArgumentException.class, () -> service.startExamAttempt(studentId, req));
    }

    @Test
    void startExamAttempt_happyPath_createsInProgressAttempt() {
        Long studentId = 1L;
        StartExamAttemptRequest req = new StartExamAttemptRequest();
        req.setExamId(10L);

        Exam exam = new Exam();
        exam.setId(10L);
        exam.setStatus(Exam.ExamStatus.PUBLISHED);

        ExamAssignment assignment = new ExamAssignment();
        assignment.setExamId(10L);
        assignment.setStudentId(studentId);

        ExamAttempt saved = new ExamAttempt();
        saved.setId(777L);
        saved.setExamId(10L);
        saved.setStudentId(studentId);
        saved.setStatus(ExamAttempt.AttemptStatus.IN_PROGRESS);

        when(examRepository.findById(10L)).thenReturn(Optional.of(exam));
        when(examAssignmentRepository.findByExamIdAndStudentId(10L, studentId)).thenReturn(Optional.of(assignment));
        when(examAttemptRepository.findAllByExamIdAndStudentId(10L, studentId)).thenReturn(List.of());
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenReturn(saved);

        ExamAttemptResponse response = service.startExamAttempt(studentId, req);

        assertNotNull(response);
        assertEquals(777L, response.getId());
        assertEquals("IN_PROGRESS", response.getStatus());

        ArgumentCaptor<ExamAttempt> attemptCaptor = ArgumentCaptor.forClass(ExamAttempt.class);
        verify(examAttemptRepository).save(attemptCaptor.capture());
        assertEquals(studentId, attemptCaptor.getValue().getStudentId());
        assertEquals(10L, attemptCaptor.getValue().getExamId());
        assertEquals(ExamAttempt.AttemptStatus.IN_PROGRESS, attemptCaptor.getValue().getStatus());
    }
}
