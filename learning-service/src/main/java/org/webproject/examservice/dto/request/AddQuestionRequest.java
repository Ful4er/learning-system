package org.webproject.examservice.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AddQuestionRequest {
    private String text;
    private String type; // SINGLE_CHOICE, MULTIPLE_CHOICE, TEXT
    private Integer points;
    private List<OptionPayload> options;

    @Data
    public static class OptionPayload {
        private String text;
        private Boolean isCorrect;
        private Integer orderIndex;
    }
}