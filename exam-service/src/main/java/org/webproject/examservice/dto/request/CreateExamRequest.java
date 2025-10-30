package org.webproject.examservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateExamRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Teacher ID is required")
    private Long teacherId;
    
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    
    @Positive(message = "Passing score must be positive")
    private Integer passingScore;
}