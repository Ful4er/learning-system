package org.webproject.examservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class AddQuestionRequest {
    @NotBlank(message = "Question text is required")
    private String text;
    
    @NotNull(message = "Question type is required")
    private String type; // SINGLE_CHOICE, MULTIPLE_CHOICE, TEXT
    
    @Positive(message = "Points must be positive")
    private Integer points = 1;
    
    @Valid
    private List<OptionPayload> options;

    @Data
    public static class OptionPayload {
        @NotBlank(message = "Option text is required")
        private String text;
        
        @NotNull(message = "isCorrect flag is required")
        private Boolean isCorrect;
        
        @Positive(message = "Order index must be positive")
        private Integer orderIndex;
    }
}