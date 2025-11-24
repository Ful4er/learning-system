package org.webproject.examservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.webproject.examservice.dto.response.StudentExamResultResponse;
import org.webproject.examservice.dto.response.UserDto;
import org.webproject.examservice.service.TeacherExamService;
import org.webproject.examservice.util.JwtUtil;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/students")
public class TeacherStudentExamController {

    private final TeacherExamService examService;
    private final JwtUtil jwtUtil;

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping
    public ResponseEntity<java.util.List<UserDto>> getTeacherStudents(Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        java.util.List<UserDto> students = examService.getStudentsByTeacher(teacherId);
        return ResponseEntity.ok(students);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/{studentId}/results")
    public ResponseEntity<List<StudentExamResultResponse>> getStudentResults(@PathVariable Long studentId, Authentication authentication) {
        Long teacherId = jwtUtil.getUserId(authentication);
        return ResponseEntity.ok(examService.getStudentResultsForTeacher(teacherId, studentId));
    }
}
