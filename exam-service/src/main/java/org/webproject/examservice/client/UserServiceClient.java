package org.webproject.examservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.webproject.examservice.dto.response.UserDto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public UserDto getUserById(Long userId) {
        try {
            String url = "/api/users/" + userId;
            log.info("Requesting user by ID: {}", url);

            Mono<String> responseMono = webClient.get()
                    .uri(url)
                    .headers(headers -> headers.addAll(buildAuthHeaders()))
                    .retrieve()
                    .bodyToMono(String.class);

            String rawResponse = responseMono.block();
            if (rawResponse == null) {
                log.warn("Empty response from user service for id {}", userId);
                return createFallbackUser(userId);
            }

            JsonNode userNode = objectMapper.readTree(rawResponse);
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
        } catch (WebClientResponseException e) {
            log.warn("User service responded with status {} for id {}", e.getStatusCode(), userId);
            return createFallbackUser(userId);
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
            String sanitizedEmail = (email == null) ? null : email.replaceAll("\\p{C}", "").trim();

            log.info("Searching user by email: {}", sanitizedEmail);

            Mono<String> responseMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/users/search")
                            .queryParam("email", sanitizedEmail)
                            .build())
                    .headers(headers -> headers.addAll(buildAuthHeaders()))
                    .retrieve()
                    .bodyToMono(String.class);

            String rawBody = responseMono.block();
            log.info("Raw search response: {}", rawBody);

            if (rawBody == null) {
                log.warn("Empty response body from user-service search for email: {}", sanitizedEmail);
                return null;
            }
            JsonNode jsonArray = objectMapper.readTree(rawBody);

            if (jsonArray.isArray() && !jsonArray.isEmpty()) {
                JsonNode firstUser = jsonArray.get(0);

                UserDto userDto = new UserDto();
                userDto.setId(firstUser.has("id") ? firstUser.get("id").asLong() : null);
                userDto.setFirstName(firstUser.has("firstName") ? firstUser.get("firstName").asText() : "Unknown");
                userDto.setLastName(firstUser.has("lastName") ? firstUser.get("lastName").asText() : "Student");
                userDto.setEmail(firstUser.has("email") ? firstUser.get("email").asText() : sanitizedEmail);

                if (firstUser.has("role")) {
                    userDto.setRole(firstUser.get("role").asText());
                } else {
                    userDto.setRole("STUDENT");
                }

                log.info("Found user by email: {} {}", userDto.getFirstName(), userDto.getLastName());
                return userDto;
            }

            log.warn("No users found for email: {} (sanitized: {})", email, sanitizedEmail);

            if (sanitizedEmail != null && sanitizedEmail.contains("@")) {
                String local = sanitizedEmail.substring(0, sanitizedEmail.indexOf('@'));
                log.info("Fallback search with local part: {}", local);
                
                Mono<String> fallbackResponseMono = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/users/search")
                                .queryParam("email", local)
                                .build())
                        .headers(headers -> headers.addAll(buildAuthHeaders()))
                        .retrieve()
                        .bodyToMono(String.class);
                
                String fallbackBody = fallbackResponseMono.block();
                if (fallbackBody != null) {
                    JsonNode fallbackJson = objectMapper.readTree(fallbackBody);
                    if (fallbackJson.isArray() && !fallbackJson.isEmpty()) {
                        JsonNode firstUser = fallbackJson.get(0);
                        UserDto userDto = new UserDto();
                        userDto.setId(firstUser.has("id") ? firstUser.get("id").asLong() : null);
                        userDto.setFirstName(firstUser.has("firstName") ? firstUser.get("firstName").asText() : "Unknown");
                        userDto.setLastName(firstUser.has("lastName") ? firstUser.get("lastName").asText() : "Student");
                        userDto.setEmail(firstUser.has("email") ? firstUser.get("email").asText() : sanitizedEmail);
                        if (firstUser.has("role")) {
                            userDto.setRole(firstUser.get("role").asText());
                        } else {
                            userDto.setRole("STUDENT");
                        }
                        log.info("Fallback found user by local part: {} {}", userDto.getFirstName(), userDto.getLastName());
                        return userDto;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch user by email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
            String tokenValue = jwtAuth.getToken().getTokenValue();
            if (tokenValue != null && !tokenValue.isEmpty()) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue);
            }
        }
        return headers;
    }
}
