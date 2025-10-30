package org.webproject.examservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateExamRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    
    @Positive(message = "Passing score must be positive")
    private Integer passingScore;
}


