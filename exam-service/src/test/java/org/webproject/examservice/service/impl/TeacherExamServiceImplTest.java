package org.webproject.examservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webproject.examservice.client.UserServiceClient;
import org.webproject.examservice.dto.response.ExamAssignmentResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.UserDto;
import org.webproject.examservice.model.Exam;
import org.webproject.examservice.model.ExamAssignment;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherExamServiceImplTest {

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
    @Mock
    private UserServiceClient userServiceClient;

    private TeacherExamServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TeacherExamServiceImpl(
                examRepository,
                examAssignmentRepository,
                questionRepository,
                questionOptionRepository,
                examAttemptRepository,
                studentAnswerRepository,
                userServiceClient
        );
    }

    @Test
    void publishExam_whenAlreadyPublished_isIdempotent() {
        Long examId = 10L;
        Long teacherId = 20L;
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setTeacherId(teacherId);
        exam.setStatus(Exam.ExamStatus.PUBLISHED);

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        ExamResponse response = service.publishExam(examId, teacherId);

        assertNotNull(response);
        assertEquals(examId, response.getId());
        assertEquals("PUBLISHED", response.getStatus());
        verify(examRepository, never()).save(any());
    }

    @Test
    void getExamAssignments_fetchesUsersInBatchOnce() {
        Long examId = 1L;
        Long teacherId = 2L;
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setTeacherId(teacherId);

        ExamAssignment a1 = new ExamAssignment();
        a1.setId(100L);
        a1.setExamId(examId);
        a1.setStudentId(11L);
        ExamAssignment a2 = new ExamAssignment();
        a2.setId(200L);
        a2.setExamId(examId);
        a2.setStudentId(22L);

        UserDto u1 = new UserDto();
        u1.setId(11L);
        u1.setFirstName("A");
        u1.setLastName("B");
        u1.setEmail("a@b.com");

        UserDto u2 = new UserDto();
        u2.setId(22L);
        u2.setFirstName("C");
        u2.setLastName("D");
        u2.setEmail("c@d.com");

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examAssignmentRepository.findAllByExamId(examId)).thenReturn(List.of(a1, a2));
        when(userServiceClient.getUsersByIds(eq(List.of(11L, 22L)))).thenReturn(List.of(u1, u2));
        when(examAttemptRepository.findAllByExamIdAndStudentIdIn(eq(examId), any())).thenReturn(List.of());

        List<ExamAssignmentResponse> result = service.getExamAssignments(examId, teacherId);

        assertEquals(2, result.size());
        assertEquals("A B", result.get(0).getStudentName());
        assertEquals("C D", result.get(1).getStudentName());
        verify(userServiceClient, times(1)).getUsersByIds(eq(List.of(11L, 22L)));
        verify(userServiceClient, never()).getUserById(any());
    }
}
