package org.webproject.examservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.webproject.examservice.dto.response.UserDto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.user-service.url:http://api-gateway:8080}")
    private String userServiceUrl;

    public UserDto getUserById(Long userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            log.info("Requesting user by ID: {}", url);

            ResponseEntity<String> rawResponse = restTemplate.getForEntity(url, String.class);
            if (!rawResponse.getStatusCode().is2xxSuccessful() || rawResponse.getBody() == null) {
                log.warn("User service responded with status {} for id {}", rawResponse.getStatusCode(), userId);
                return createFallbackUser(userId);
            }

            JsonNode userNode = objectMapper.readTree(rawResponse.getBody());
            if (userNode == null || userNode.isNull()) {
                log.warn("Empty user payload received for id {}", userId);
                return createFallbackUser(userId);
            }

            UserDto userDto = new UserDto();
            userDto.setId(userNode.has("id") ? userNode.get("id").asLong() : userId);
            userDto.setFirstName(userNode.has("firstName") ? userNode.get("firstName").asText() : "Unknown");
            userDto.setLastName(userNode.has("lastName") ? userNode.get("lastName").asText() : "Unknown");
            userDto.setEmail(userNode.has("email") ? userNode.get("email").asText() : "Unknown");
            userDto.setRole(userNode.has("role") ? userNode.get("role").asText() : "STUDENT");

            log.info("Resolved user {} {} via user-service", userDto.getFirstName(), userDto.getLastName());
            return userDto;
        } catch (Exception e) {
            log.error("Failed to fetch user by ID {}: {}", userId, e.getMessage(), e);
            return createFallbackUser(userId);
        }
    }
    private UserDto createFallbackUser(Long userId) {
        UserDto fallback = new UserDto();
        fallback.setId(userId);
        fallback.setFirstName("Student");
        fallback.setLastName("Unknown");
        fallback.setEmail("student" + userId + "@example.com");
        fallback.setRole("STUDENT");
        return fallback;
    }

    public UserDto getUserByEmail(String email) {
        try {
            String url = userServiceUrl + "/api/users/search?email=" +
                    java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);

            log.info("Searching user by email: {}", url);

            ResponseEntity<String> rawResponse = restTemplate.getForEntity(url, String.class);
            log.info("Raw search response: {}", rawResponse.getBody());

            JsonNode jsonArray = objectMapper.readTree(rawResponse.getBody());

            if (jsonArray.isArray() && jsonArray.size() > 0) {
                JsonNode firstUser = jsonArray.get(0);

                UserDto userDto = new UserDto();
                userDto.setId(firstUser.has("id") ? firstUser.get("id").asLong() : null);
                userDto.setFirstName(firstUser.has("firstName") ? firstUser.get("firstName").asText() : "Unknown");
                userDto.setLastName(firstUser.has("lastName") ? firstUser.get("lastName").asText() : "Student");
                userDto.setEmail(firstUser.has("email") ? firstUser.get("email").asText() : email);

                if (firstUser.has("role")) {
                    userDto.setRole(firstUser.get("role").asText());
                } else {
                    userDto.setRole("STUDENT");
                }

                log.info("Found user by email: {} {}", userDto.getFirstName(), userDto.getLastName());
                return userDto;
            }

            log.warn("No users found for email: {}", email);
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch user by email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }
}