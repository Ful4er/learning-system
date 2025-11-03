package org.webproject.examservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webproject.examservice.dto.request.AddQuestionRequest;
import org.webproject.examservice.dto.request.CreateExamRequest;
import org.webproject.examservice.dto.request.UpdateExamRequest;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;
import org.webproject.examservice.exception.AccessDeniedException;
import org.webproject.examservice.exception.ExamAlreadyPublishedException;
import org.webproject.examservice.exception.ExamNotFoundException;
import org.webproject.examservice.model.Exam;
import org.webproject.examservice.model.Question;
import org.webproject.examservice.repository.ExamAssignmentRepository;
import org.webproject.examservice.repository.ExamRepository;
import org.webproject.examservice.repository.QuestionOptionRepository;
import org.webproject.examservice.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private TeacherExamServiceImpl service;

    private Exam draftExam;

    @BeforeEach
    void setUp() {
        draftExam = new Exam();
        draftExam.setId(1L);
        draftExam.setTeacherId(10L);
        draftExam.setTitle("Draft");
        draftExam.setStatus(Exam.ExamStatus.DRAFT);
        draftExam.setDurationMinutes(60);
        draftExam.setPassingScore(70);
    }

    @Test
    void createExam_savesAndReturnsResponse() {
        CreateExamRequest req = new CreateExamRequest();
        req.setTitle("New Exam");
        req.setDescription("Desc");
        req.setTeacherId(10L);
        req.setDurationMinutes(45);
        req.setPassingScore(60);

        Exam saved = new Exam();
        saved.setId(5L);
        saved.setTitle(req.getTitle());
        saved.setDescription(req.getDescription());
        saved.setTeacherId(10L);
        saved.setDurationMinutes(45);
        saved.setPassingScore(60);
        saved.setStatus(Exam.ExamStatus.DRAFT);

        when(examRepository.save(any(Exam.class))).thenReturn(saved);
        when(questionRepository.findAllByExamIdOrderByIdAsc(5L)).thenReturn(List.of());
        when(examAssignmentRepository.findAllByExamId(5L)).thenReturn(List.of());

        ExamResponse response = service.createExam(req);

        assertEquals(5L, response.getId());
        assertEquals("New Exam", response.getTitle());
        verify(examRepository, times(1)).save(any(Exam.class));
    }

    @Test
    void getExamById_notFound_throws() {
        when(examRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ExamNotFoundException.class, () -> service.getExamById(99L));
    }

    @Test
    void updateExam_ownerAndDraft_updates() {
        UpdateExamRequest req = new UpdateExamRequest();
        req.setTitle("Updated");
        req.setDescription("D");
        req.setDurationMinutes(30);
        req.setPassingScore(50);

        when(examRepository.findById(1L)).thenReturn(Optional.of(draftExam));
        when(examRepository.save(any(Exam.class))).thenAnswer(i -> i.getArgument(0));
        when(questionRepository.findAllByExamIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(examAssignmentRepository.findAllByExamId(1L)).thenReturn(List.of());

        ExamResponse r = service.updateExam(1L, 10L, req);

        assertEquals("Updated", r.getTitle());
        ArgumentCaptor<Exam> examCaptor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(examCaptor.capture());
        assertEquals(30, examCaptor.getValue().getDurationMinutes());
    }

    @Test
    void updateExam_wrongOwner_throwsAccessDenied() {
        when(examRepository.findById(1L)).thenReturn(Optional.of(draftExam));
        assertThrows(AccessDeniedException.class, () -> service.updateExam(1L, 11L, new UpdateExamRequest()));
    }

    @Test
    void publishExam_alreadyPublished_throws() {
        Exam published = new Exam();
        published.setId(2L);
        published.setTeacherId(10L);
        published.setStatus(Exam.ExamStatus.PUBLISHED);
        when(examRepository.findById(2L)).thenReturn(Optional.of(published));
        assertThrows(ExamAlreadyPublishedException.class, () -> service.publishExam(2L, 10L));
    }

    @Test
    void addQuestion_createsOptions_whenProvided() {
        AddQuestionRequest req = new AddQuestionRequest();
        req.setText("Q1");
        req.setPoints(5);
        req.setType("SINGLE_CHOICE");
        List<AddQuestionRequest.OptionPayload> opts = new ArrayList<>();
        AddQuestionRequest.OptionPayload p1 = new AddQuestionRequest.OptionPayload();
        p1.setText("A");
        p1.setIsCorrect(true);
        p1.setOrderIndex(1);
        opts.add(p1);
        req.setOptions(opts);

        when(examRepository.findById(1L)).thenReturn(Optional.of(draftExam));
        Question savedQ = new Question();
        savedQ.setId(100L);
        savedQ.setExamId(1L);
        savedQ.setText("Q1");
        savedQ.setType(Question.QuestionType.SINGLE_CHOICE);
        savedQ.setPoints(5);
        when(questionRepository.save(any(Question.class))).thenReturn(savedQ);
        when(questionOptionRepository.findAllByQuestionIdOrderByOrderIndexAsc(100L)).thenReturn(List.of());

        QuestionResponse r = service.addQuestion(1L, 10L, req);

        assertEquals(100L, r.getId());
        verify(questionOptionRepository).saveAll(anyList());
    }
}


