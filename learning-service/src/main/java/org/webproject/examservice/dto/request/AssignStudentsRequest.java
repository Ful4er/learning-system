package org.webproject.examservice.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AssignStudentsRequest {
    private List<Long> studentIds;
}