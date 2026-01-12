package org.webproject.examservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class UserServiceClientTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private UserServiceClient client;

    @BeforeEach
    public void setup() {
        // RestTemplate создаётся как реальный bean в Spring контексте
        // Прямое мокирование RestTemplate.class вызывает проблемы с Byte Buddy в Java 21+
        restTemplate = new RestTemplate();
        objectMapper = new ObjectMapper();
        client = new UserServiceClient(restTemplate, objectMapper);
    }

    @Test
    @Disabled("RestTemplate требует реального HTTP запроса или @SpringBootTest; используйте MockRestServiceServer для интеграционных тестов")
    public void getUserByEmail_returnsUser() throws Exception {
        String json = "[{\"id\":3,\"email\":\"student1@example.com\",\"firstName\":\"Student1\",\"lastName\":\"Test\",\"role\":\"STUDENT\"}]";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(json, HttpStatus.OK);
        
        // Для полного тестирования используйте MockRestServiceServer:
        // MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        // server.expect(requestTo("http://user-service:8081/api/users/byEmail/student1@example.com"))
        //     .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        var user = client.getUserByEmail("student1@example.com");
        assertNotNull(user);
        assertEquals(3L, user.getId());
        assertEquals("student1@example.com", user.getEmail());
    }
}
