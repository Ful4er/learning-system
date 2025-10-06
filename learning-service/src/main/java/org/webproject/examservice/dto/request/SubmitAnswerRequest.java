package org.webproject.examservice.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SubmitAnswerRequest {
    private Long questionId;
    private List<Long> selectedOptionIds;
    private String textAnswer;
}

