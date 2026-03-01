package org.webproject.examservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.webproject.examservice.dto.response.UserDto;
import org.webproject.examservice.util.Role;

@Component
public class UserDtoMapper {
    public UserDto fromNode(JsonNode node) {
        UserDto dto = new UserDto();
        dto.setId(getLongValue(node, "id"));
        dto.setFirstName(getStringValue(node, "firstName", "Unknown"));
        dto.setLastName(getStringValue(node, "lastName", "Student"));
        dto.setEmail(getStringValue(node, "email", "Unknown"));
        dto.setRole(Role.valueOf(getStringValue(node, "role", String.valueOf(Role.STUDENT))));
        return dto;
    }

    private Long getLongValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asLong() : null;
    }

    private String getStringValue(JsonNode node, String field, String defaultValue) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText() : defaultValue;
    }
}