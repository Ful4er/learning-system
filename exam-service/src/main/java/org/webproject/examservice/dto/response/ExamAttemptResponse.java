package org.webproject.examservice.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class ExamAttemptResponse {
    private Long id;
    private Long examId;
    private Long studentId;
    private Instant startedAt;
    private Instant finishedAt;
    private String status;
    private Double calculatedScore;

    private Double score;
    private Integer totalQuestions;
    private Boolean passed;
    private String examTitle;
}


