package org.webproject.examservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.webproject.examservice.dto.request.AddQuestionRequest;
import org.webproject.examservice.dto.request.AssignStudentsRequest;
import org.webproject.examservice.dto.request.CreateExamRequest;
import org.webproject.examservice.dto.request.UpdateExamRequest;
import org.webproject.examservice.dto.request.UpdateQuestionRequest;
import org.webproject.examservice.dto.response.ExamAssignmentResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;
import org.webproject.examservice.service.TeacherExamService;
import org.webproject.examservice.util.JwtUtil;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/exams")
public class TeacherExamController {

    private final TeacherExamService examService;
    private final JwtUtil jwtUtil;

    // Exam CRUD operations
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping
    public ResponseEntity<ExamResponse> createExam(@RequestBody CreateExamRequest request, Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        request.setTeacherId(teacherId);
        return ResponseEntity.ok(examService.createExam(request));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{examId}")
    public ResponseEntity<ExamResponse> getExam(@PathVariable Long examId) {
        return ResponseEntity.ok(examService.getExamById(examId));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{examId}")
    public ResponseEntity<ExamResponse> updateExam(@PathVariable Long examId,
                                                  @RequestBody UpdateExamRequest request,
                                                  Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.updateExam(examId, teacherId, request));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long examId, Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        examService.deleteExam(examId, teacherId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{examId}/publish")
    public ResponseEntity<ExamResponse> publishExam(@PathVariable Long examId, Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.publishExam(examId, teacherId));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{examId}/archive")
    public ResponseEntity<ExamResponse> archiveExam(@PathVariable Long examId, Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.archiveExam(examId, teacherId));
    }

    // Teacher exam management
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping
    public ResponseEntity<List<ExamResponse>> getMyExams(Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getExamsByTeacher(teacherId));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{examId}/assignments")
    public ResponseEntity<List<ExamAssignmentResponse>> getExamAssignments(@PathVariable Long examId,
                                                                          Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getExamAssignments(examId, teacherId));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{examId}/assign")
    public ResponseEntity<Void> assignStudents(@PathVariable Long examId,
                                              @RequestBody AssignStudentsRequest request,
                                              Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        examService.assignStudents(examId, teacherId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{examId}/assignments/{studentId}")
    public ResponseEntity<Void> removeStudentAssignment(@PathVariable Long examId,
                                                       @PathVariable Long studentId,
                                                       Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        examService.removeStudentAssignment(examId, studentId, teacherId);
        return ResponseEntity.ok().build();
    }

    // Question management
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/{examId}/questions")
    public ResponseEntity<QuestionResponse> addQuestion(@PathVariable Long examId,
                                                       @RequestBody AddQuestionRequest request,
                                                       Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.addQuestion(examId, teacherId, request));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable Long questionId) {
        return ResponseEntity.ok(examService.getQuestionById(questionId));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponse> updateQuestion(@PathVariable Long questionId,
                                                          @RequestBody UpdateQuestionRequest request,
                                                          Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.updateQuestion(questionId, teacherId, request));
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long questionId, Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        examService.deleteQuestion(questionId, teacherId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{examId}/questions")
    public ResponseEntity<List<QuestionResponse>> getExamQuestions(@PathVariable Long examId,
                                                                  Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getExamQuestionsForTeacher(examId, teacherId));
    }
}


