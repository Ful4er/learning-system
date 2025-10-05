package org.webproject.examservice.service;

import org.webproject.examservice.dto.request.AddQuestionRequest;
import org.webproject.examservice.dto.request.AssignStudentsRequest;
import org.webproject.examservice.dto.request.CreateExamRequest;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;

import java.util.List;

public interface ExamService {
    ExamResponse createExam(CreateExamRequest request);
    ExamResponse publishExam(Long examId, Long teacherId);
    void assignStudents(Long examId, Long teacherId, AssignStudentsRequest request);
    QuestionResponse addQuestion(Long examId, Long teacherId, AddQuestionRequest request);

    List<ExamResponse> getAssignedExamsForStudent(Long studentId);
    ExamResponse getExamDetails(Long examId, Long requesterId);
    List<QuestionResponse> getExamQuestions(Long examId, Long requesterId);
}


