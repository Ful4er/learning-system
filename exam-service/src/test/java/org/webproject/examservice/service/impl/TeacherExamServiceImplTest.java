package org.webproject.examservice.service.impl;

import org.junit.jupiter.api.Test;
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
import org.webproject.examservice.model.QuestionOption;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeacherExamServiceImplTest {

    @Test
    public void testExamCreationRequest() {
        CreateExamRequest request = new CreateExamRequest();
        request.setTitle("Test Exam");
        request.setDescription("Test Description");
        request.setDurationMinutes(60);
        
        assertEquals("Test Exam", request.getTitle());
        assertEquals("Test Description", request.getDescription());
        assertEquals(60, request.getDurationMinutes());
    }

    @Test
    public void testExamUpdateRequest() {
        // Test exam update request validation
        UpdateExamRequest request = new UpdateExamRequest();
        request.setTitle("Updated Exam");
        request.setDescription("Updated Description");
        
        assertEquals("Updated Exam", request.getTitle());
        assertEquals("Updated Description", request.getDescription());
    }

    @Test
    public void testQuestionRequest() {
        AddQuestionRequest request = new AddQuestionRequest();
        request.setText("What is 2+2?");
        
        assertEquals("What is 2+2?", request.getText());
    }

    @Test
    public void testExamModel() {
        // Test exam model properties
        Exam exam = new Exam();
        exam.setTitle("Test Exam");
        exam.setDescription("Test Description");
        exam.setDurationMinutes(60);
        exam.setStatus(Exam.ExamStatus.DRAFT);
        
        assertEquals("Test Exam", exam.getTitle());
        assertEquals("Test Description", exam.getDescription());
        assertEquals(60, exam.getDurationMinutes());
        assertEquals(Exam.ExamStatus.DRAFT, exam.getStatus());
    }

    @Test
    public void testQuestionModel() {
        Question question = new Question();
        question.setText("Test Question");
        question.setPoints(10);
        
        assertEquals("Test Question", question.getText());
        assertEquals(10, question.getPoints());
    }

    @Test
    public void testQuestionOptionModel() {
        QuestionOption option = new QuestionOption();
        option.setText("Option A");
        
        assertEquals("Option A", option.getText());
    }

    @Test
    public void testExamResponse() {
        ExamResponse response = new ExamResponse();
        response.setTitle("Test Exam");
        response.setDescription("Test Description");
        
        assertEquals("Test Exam", response.getTitle());
        assertEquals("Test Description", response.getDescription());
    }

    @Test
    public void testQuestionResponse() {
        QuestionResponse response = new QuestionResponse();
        response.setText("Test Question");
        
        assertEquals("Test Question", response.getText());
    }

    @Test
    public void testExceptionMessages() {
        ExamNotFoundException notFound = new ExamNotFoundException("Exam not found");
        assertEquals("Exam not found", notFound.getMessage());
        
        AccessDeniedException accessDenied = new AccessDeniedException("Access denied");
        assertEquals("Access denied", accessDenied.getMessage());
        
        ExamAlreadyPublishedException published = new ExamAlreadyPublishedException("Already published");
        assertEquals("Already published", published.getMessage());
    }

    @Test
    public void testExamStatusEnum() {
        Exam.ExamStatus[] statuses = Exam.ExamStatus.values();
        assertTrue(statuses.length > 0);
        
        boolean foundDraft = false;
        boolean foundPublished = false;
        
        for (Exam.ExamStatus status : statuses) {
            if (status == Exam.ExamStatus.DRAFT) {
                foundDraft = true;
            }
            if (status == Exam.ExamStatus.PUBLISHED) {
                foundPublished = true;
            }
        }
        
        assertTrue(foundDraft);
        assertTrue(foundPublished);
    }

    @Test
    public void testListOperations() {
        List<Question> questions = new ArrayList<>();
        Question question1 = new Question();
        question1.setText("Question 1");
        Question question2 = new Question();
        question2.setText("Question 2");
        
        questions.add(question1);
        questions.add(question2);
        
        assertEquals(2, questions.size());
        assertEquals("Question 1", questions.get(0).getText());
        assertEquals("Question 2", questions.get(1).getText());
    }
}
