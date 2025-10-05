package org.webproject.examservice.dto.request;

import lombok.Data;

@Data
public class CreateExamRequest {
    private String title;
    private String description;
    private Long teacherId;
    private Integer durationMinutes;
    private Integer passingScore;
}