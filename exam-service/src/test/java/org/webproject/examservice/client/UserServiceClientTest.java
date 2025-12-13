package org.webproject.examservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserServiceClientTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private UserServiceClient client;

    @BeforeEach
    public void setup() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        client = new UserServiceClient(restTemplate, objectMapper);
    }

    @Test
    public void getUserByEmail_returnsUser() throws Exception {
        String json = "[{\"id\":3,\"email\":\"student1@example.com\",\"firstName\":\"Student1\",\"lastName\":\"Test\",\"role\":\"STUDENT\"}]";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(json, HttpStatus.OK);
        when(restTemplate.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.eq(HttpMethod.GET), ArgumentMatchers.any(), ArgumentMatchers.eq(String.class)))
                .thenReturn(responseEntity);

        var user = client.getUserByEmail("student1@example.com");
        assertNotNull(user);
        assertEquals(3L, user.getId());
        assertEquals("student1@example.com", user.getEmail());
    }
}
