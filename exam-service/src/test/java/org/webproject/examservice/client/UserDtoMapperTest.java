package org.webproject.examservice.client;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserDtoMapper mapper = new UserDtoMapper();

    @Test
    void fromNode_mapsAllFields() throws Exception {
        String userJson = "{\"id\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@example.com\",\"role\":\"STUDENT\"}";
        var userNode = objectMapper.readTree(userJson);

        var dto = mapper.fromNode(userNode);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("STUDENT", dto.getRole());
    }

    @Test
    void fromNode_appliesDefaults_whenMissingFields() throws Exception {
        String userJson = "{\"id\":5}";
        var userNode = objectMapper.readTree(userJson);

        var dto = mapper.fromNode(userNode);

        assertNotNull(dto);
        assertEquals(5L, dto.getId());
        assertEquals("Unknown", dto.getFirstName());
        assertEquals("Student", dto.getLastName());
        assertEquals("Unknown", dto.getEmail());
        assertEquals("STUDENT", dto.getRole());
    }

    @Test
    void fromNode_handlesNullValues_asDefaults() throws Exception {
        String userJson = "{\"id\":null,\"firstName\":null,\"lastName\":null,\"email\":null,\"role\":null}";
        var userNode = objectMapper.readTree(userJson);

        var dto = mapper.fromNode(userNode);

        assertNotNull(dto);
        assertNull(dto.getId());
        assertEquals("Unknown", dto.getFirstName());
        assertEquals("Student", dto.getLastName());
        assertEquals("Unknown", dto.getEmail());
        assertEquals("STUDENT", dto.getRole());
    }
}
