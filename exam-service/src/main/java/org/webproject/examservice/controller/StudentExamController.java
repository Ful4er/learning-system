package org.webproject.examservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.webproject.examservice.dto.request.StartExamAttemptRequest;
import org.webproject.examservice.dto.request.SubmitAnswerRequest;
import org.webproject.examservice.dto.response.ExamAttemptResponse;
import org.webproject.examservice.dto.response.ExamResponse;
import org.webproject.examservice.dto.response.QuestionResponse;
import org.webproject.examservice.service.StudentExamService;
import org.webproject.examservice.util.JwtUtil;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/exams")
public class StudentExamController {

    private final StudentExamService examService;
    private final JwtUtil jwtUtil;

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<List<ExamResponse>> getMyExams(Authentication authentication) {
        Long studentId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getAssignedExamsForStudent(studentId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{examId}")
    public ResponseEntity<ExamResponse> getExamDetails(@PathVariable Long examId, Authentication authentication) {
        Long studentId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getExamDetails(examId, studentId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{examId}/questions")
    public ResponseEntity<List<QuestionResponse>> getExamQuestions(@PathVariable Long examId, Authentication authentication) {
        Long studentId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getExamQuestions(examId, studentId));
    }

    // Exam attempt endpoints
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/attempts")
    public ResponseEntity<ExamAttemptResponse> startExamAttempt(@RequestBody StartExamAttemptRequest request, Authentication authentication) {
        Long studentId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.startExamAttempt(studentId, request));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/attempts/{attemptId}/finish")
    public ResponseEntity<ExamAttemptResponse> finishExamAttempt(@PathVariable Long attemptId, Authentication authentication) {
        Long studentId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.finishExamAttempt(studentId, attemptId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{examId}/current-attempt")
    public ResponseEntity<ExamAttemptResponse> getCurrentAttempt(@PathVariable Long examId, Authentication authentication) {
        Long studentId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getCurrentAttempt(studentId, examId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<Void> submitAnswer(@PathVariable Long attemptId, @RequestBody SubmitAnswerRequest request, Authentication authentication) {
        Long studentId = jwtUtil.getUserId(authentication);
        examService.submitAnswer(studentId, attemptId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{examId}/attempts")
    public ResponseEntity<List<ExamAttemptResponse>> getStudentAttempts(@PathVariable Long examId, Authentication authentication) {
        Long studentId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getStudentAttempts(studentId, examId));
    }
}


