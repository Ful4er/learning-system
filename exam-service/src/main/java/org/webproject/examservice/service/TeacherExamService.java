package org.webproject.examservice.service;

import org.webproject.examservice.dto.request.AddQuestionRequest;
import org.webproject.examservice.dto.request.AssignStudentsRequest;
import org.webproject.examservice.dto.request.CreateExamRequest;
import org.webproject.examservice.dto.request.UpdateExamRequest;
import org.webproject.examservice.dto.request.UpdateQuestionRequest;
import org.webproject.examservice.dto.response.ExamAssignmentResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;

import java.util.List;

public interface TeacherExamService {
    ExamResponse createExam(CreateExamRequest request);
    ExamResponse getExamById(Long examId);
    ExamResponse updateExam(Long examId, Long teacherId, UpdateExamRequest request);
    void deleteExam(Long examId, Long teacherId);
    ExamResponse publishExam(Long examId, Long teacherId);
    ExamResponse archiveExam(Long examId, Long teacherId);

    List<ExamResponse> getExamsByTeacher(Long teacherId);
    List<ExamAssignmentResponse> getExamAssignments(Long examId, Long teacherId);
    void assignStudents(Long examId, Long teacherId, AssignStudentsRequest request);
    void removeStudentAssignment(Long examId, Long studentId, Long teacherId);

    QuestionResponse addQuestion(Long examId, Long teacherId, AddQuestionRequest request);
    QuestionResponse getQuestionById(Long questionId);
    QuestionResponse updateQuestion(Long questionId, Long teacherId, UpdateQuestionRequest request);
    void deleteQuestion(Long questionId, Long teacherId);
    List<QuestionResponse> getExamQuestionsForTeacher(Long examId, Long teacherId);
}




