package org.webproject.examservice.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class ExamAssignmentResponse {
    private Long id;
    private Long examId;
    private Long studentId;
    private Instant assignedAt;
    private String studentName;
    private String studentFirstName;
    private String studentLastName;
    private String studentEmail;
    private Instant completedAt;
    private Integer score;
}