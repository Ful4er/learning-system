package org.webproject.examservice.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StudentAnswerResponse {
    private Long questionId;
    private List<Long> selectedOptionIds;
    private String textAnswer;
}

