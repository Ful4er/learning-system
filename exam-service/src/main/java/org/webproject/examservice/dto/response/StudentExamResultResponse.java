package org.webproject.examservice.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class StudentExamResultResponse {
    private Long examId;
    private String examTitle;
    private String examStatus;

    private boolean assigned;
    private Instant assignedAt;

    private int attemptsCount;
    private Long lastAttemptId;
    private String lastAttemptStatus;
    private Double lastAttemptScore;
    private Instant lastAttemptFinishedAt;

    private Integer passingScore;
}
