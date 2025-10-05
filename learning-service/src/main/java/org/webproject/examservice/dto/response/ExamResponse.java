package org.webproject.examservice.dto.response;

import lombok.Data;

@Data
public class ExamResponse {
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private Integer durationMinutes;
    private Integer passingScore;
    private String status;
}