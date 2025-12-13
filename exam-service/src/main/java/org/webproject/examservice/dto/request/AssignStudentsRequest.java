package org.webproject.examservice.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class AssignStudentsRequest {
    private List<String> studentEmails;
    private List<Long> studentIds;
}