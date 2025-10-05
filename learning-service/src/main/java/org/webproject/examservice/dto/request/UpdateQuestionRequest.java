package org.webproject.examservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class UpdateQuestionRequest {
    @NotBlank(message = "Question text is required")
    private String text;
    
    @Positive(message = "Points must be positive")
    private Integer points;
    
    @Valid
    private List<OptionPayload> options;

    @Data
    public static class OptionPayload {
        private Long id;
        @NotBlank(message = "Option text is required")
        private String text;
        
        @NotNull(message = "isCorrect flag is required")
        private Boolean isCorrect;
        
        @Positive(message = "Order index must be positive")
        private Integer orderIndex;
    }
}
