package org.webproject.examservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceClientTest {

    private UserServiceClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
        client = new UserServiceClient(webClient, objectMapper);
    }

    @Test
    public void getUserById_returnsFallbackUser_onNetworkError(){
        var user = client.getUserById(1L);
        
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("Student", user.getFirstName());
        assertEquals("Unknown", user.getLastName());
        assertEquals("student1@example.com", user.getEmail());
        assertEquals("STUDENT", user.getRole());
    }

    @Test
    public void getUserByEmail_returnsNull_onNetworkError(){
        var user = client.getUserByEmail("test@example.com");
        
        assertNull(user);
    }

    @Test
    public void testJsonParsing() throws Exception {
        String userJson = "{\"id\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@example.com\",\"role\":\"STUDENT\"}";
        var userNode = objectMapper.readTree(userJson);
        
        assertEquals(1, userNode.get("id").asLong());
        assertEquals("John", userNode.get("firstName").asText());
        assertEquals("Doe", userNode.get("lastName").asText());
        assertEquals("john@example.com", userNode.get("email").asText());
        assertEquals("STUDENT", userNode.get("role").asText());
    }

    @Test
    public void testUserArrayJsonParsing() throws Exception {
        // Test that our ObjectMapper can parse the user array JSON responses
        String usersJson = "[{\"id\":3,\"email\":\"student1@example.com\",\"firstName\":\"Student1\",\"lastName\":\"Test\",\"role\":\"STUDENT\"}]";
        var usersNode = objectMapper.readTree(usersJson);
        
        assertTrue(usersNode.isArray());
        assertEquals(1, usersNode.size());
        
        var firstUser = usersNode.get(0);
        assertEquals(3, firstUser.get("id").asLong());
        assertEquals("student1@example.com", firstUser.get("email").asText());
        assertEquals("Student1", firstUser.get("firstName").asText());
        assertEquals("Test", firstUser.get("lastName").asText());
        assertEquals("STUDENT", firstUser.get("role").asText());
    }
}
