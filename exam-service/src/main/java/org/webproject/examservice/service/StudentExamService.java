package org.webproject.examservice.service;

import org.webproject.examservice.dto.request.StartExamAttemptRequest;
import org.webproject.examservice.dto.request.SubmitAnswerRequest;
import org.webproject.examservice.dto.response.ExamAttemptResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;

import java.util.List;

public interface StudentExamService {
    List<ExamResponse> getAssignedExamsForStudent(Long studentId);
    ExamResponse getExamDetails(Long examId, Long studentId);
    List<QuestionResponse> getExamQuestions(Long examId, Long studentId);
    
    // Exam attempt functionality
    ExamAttemptResponse startExamAttempt(Long studentId, StartExamAttemptRequest request);
    ExamAttemptResponse finishExamAttempt(Long studentId, Long attemptId);
    ExamAttemptResponse getCurrentAttempt(Long studentId, Long examId);
    void submitAnswer(Long studentId, Long attemptId, SubmitAnswerRequest request);
    List<ExamAttemptResponse> getStudentAttempts(Long studentId, Long examId);
}


