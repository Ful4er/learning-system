package org.webproject.examservice.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class QuestionResponse {
    private Long id;
    private Long examId;
    private String text;
    private String type;
    private Integer points;
    private List<Option> options;

    @Data
    public static class Option {
        private Long id;
        private String text;
        private Integer orderIndex;
    }
}


