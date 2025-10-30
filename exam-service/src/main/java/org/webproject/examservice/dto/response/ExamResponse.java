package org.webproject.examservice.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class ExamResponse {
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer durationMinutes;
    private Integer passingScore;
    private String status;
    private Integer questionCount;
    private Integer assignedStudentCount;
}