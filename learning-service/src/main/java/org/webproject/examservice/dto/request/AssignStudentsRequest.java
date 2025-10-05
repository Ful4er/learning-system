package org.webproject.examservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignStudentsRequest {
    @NotEmpty(message = "Student IDs list cannot be empty")
    @NotNull(message = "Student IDs list is required")
    private List<Long> studentIds;
}