package org.webproject.examservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.webproject.examservice.dto.response.TokenIntrospectionResponse;
import org.webproject.examservice.dto.response.UserDto;
import org.webproject.examservice.util.Role;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    @Value("${api.user-service.user-url}")
    private String userUrl;
    @Value("${api.user-service.user-search-url}")
    private String userSearchUrl;
    @Value("${api.user-service.introspection-url}")
    private String authIntrospectionUrl;
    @Value("${api.user-service.user-batch-url:${api.user-service.user-url}/batch}")
    private String userBatchUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final UserDtoMapper userDtoMapper;

    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public UserDto getUserById(Long userId) {
        try {
            String dtoUrl = userUrl + userId + "/dto";

            Mono<String> responseMono = webClient.get()
                    .uri(dtoUrl)
                    .headers(headers -> headers.addAll(buildAuthHeaders()))
                    .retrieve()
                    .bodyToMono(String.class);

            String rawResponse = responseMono.block();
            if (rawResponse == null) {
                log.warn("Empty response from user service DTO endpoint for id {}", userId);
                return createFallbackByUserId(userId);
            }

            JsonNode userNode = objectMapper.readTree(rawResponse);
            if (userNode == null || userNode.isNull()) {
                log.warn("Empty user payload received for id {}", userId);
                return createFallbackByUserId(userId);
            }

            UserDto userDto = userDtoMapper.fromNode(userNode);
            log.info("Resolved user {} {} via user-service DTO endpoint",
                    userDto.getFirstName(), userDto.getLastName());
            return userDto;
        } catch (WebClientResponseException e) {
            log.warn("User service responded with status {} for id {}", e.getStatusCode(), userId);
            return createFallbackByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to fetch user by ID {}: {}", userId, e.getMessage(), e);
            return createFallbackByUserId(userId);
        }
    }

    public List<UserDto> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(ids));

        final int batchSize = 100;
        Map<Long, UserDto> resolvedById = new HashMap<>(uniqueIds.size());
        for (int from = 0; from < uniqueIds.size(); from += batchSize) {
            int to = Math.min(from + batchSize, uniqueIds.size());
            List<Long> batch = uniqueIds.subList(from, to);

            List<UserDto> batchUsers = fetchUsersByIdsBatch(batch);
            for (UserDto user : batchUsers) {
                if (user != null && user.getId() != null) {
                    resolvedById.putIfAbsent(user.getId(), user);
                }
            }
        }

        List<UserDto> result = new ArrayList<>(uniqueIds.size());
        for (Long id : uniqueIds) {
            UserDto resolved = resolvedById.get(id);
            result.add(resolved != null ? resolved : createFallbackByUserId(id));
        }

        return result;
    }

    private List<UserDto> fetchUsersByIdsBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String idsParam = ids.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            Mono<String> responseMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(userBatchUrl)
                            .queryParam("ids", idsParam)
                            .build())
                    .headers(headers -> headers.addAll(buildAuthHeaders()))
                    .retrieve()
                    .bodyToMono(String.class);

            String rawResponse = responseMono.block();
            if (rawResponse == null) {
                log.warn("Empty response from user service batch endpoint for ids: {}", ids);
                return createFallbackUsersByIds(ids);
            }

            JsonNode usersArray = objectMapper.readTree(rawResponse);
            if (!usersArray.isArray()) {
                log.warn("Response is not an array from batch endpoint");
                return createFallbackUsersByIds(ids);
            }

            List<UserDto> users = new ArrayList<>();
            for (JsonNode userNode : usersArray) {
                users.add(userDtoMapper.fromNode(userNode));
            }

            if (users.size() < ids.size()) {
                Set<Long> foundIds = users.stream()
                        .map(UserDto::getId)
                        .collect(Collectors.toSet());

                List<Long> missingIds = ids.stream()
                        .filter(id -> !foundIds.contains(id))
                        .collect(Collectors.toList());

                if (!missingIds.isEmpty()) {
                    log.warn("Users not found for ids: {}. Creating fallbacks.", missingIds);
                    users.addAll(createFallbackUsersByIds(missingIds));
                }
            }

            return users;
        } catch (WebClientResponseException e) {
            log.warn("User service batch endpoint responded with status {} for ids: {}",
                    e.getStatusCode(), ids);
            return createFallbackUsersByIds(ids);
        } catch (Exception e) {
            log.error("Failed to fetch users by ids {}: {}", ids, e.getMessage(), e);
            return createFallbackUsersByIds(ids);
        }
    }

    private UserDto createFallbackByUserId(Long userId) {
        UserDto fallback = new UserDto();
        fallback.setId(userId);
        fallback.setFirstName("Student");
        fallback.setLastName("Unknown");
        fallback.setEmail("student" + userId + "@example.com");
        fallback.setRole(Role.STUDENT);
        fallback.setCreatedAt(LocalDateTime.now());
        return fallback;
    }

    private List<UserDto> createFallbackUsersByIds(List<Long> ids) {
        return ids.stream()
                .map(this::createFallbackByUserId)
                .toList();
    }

    public UserDto getUserByEmail(@NotNull String email) {
        try {
            String sanitizedEmail = email.replaceAll("\\p{C}", "").trim();

            Mono<String> responseMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(userSearchUrl)
                            .queryParam("email", sanitizedEmail)
                            .build())
                    .headers(headers -> headers.addAll(buildAuthHeaders()))
                    .retrieve()
                    .bodyToMono(String.class);

            String rawBody = responseMono.block();
            log.debug("Raw search response: {}", rawBody);

            if (rawBody == null) {
                log.warn("Empty response body from user-service search for email: {}", sanitizedEmail);
                return null;
            }
            JsonNode jsonArray = objectMapper.readTree(rawBody);

            if (jsonArray.isArray() && !jsonArray.isEmpty()) {
                JsonNode firstUser = jsonArray.get(0);
                return userDtoMapper.fromNode(firstUser);
            }

            log.warn("No users found for email: {} ", email);
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch user by email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    public TokenIntrospectionResponse introspectToken(String token) {
        try {
            Mono<TokenIntrospectionResponse> responseMono = webClient.post()
                    .uri(authIntrospectionUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(TokenIntrospectionResponse.class);

            TokenIntrospectionResponse response = responseMono.block();
            if (response == null) {
                log.warn("Empty introspection response from user-service");
                return new TokenIntrospectionResponse(false, null, null, null, null, null);
            }
            return response;
        } catch (WebClientResponseException e) {
            log.warn("User-service introspection responded with status {}: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return new TokenIntrospectionResponse(false, null, null, null, null, null);
        } catch (Exception e) {
            log.error("Failed to introspect token via user-service: {}", e.getMessage(), e);
            return new TokenIntrospectionResponse(false, null, null, null, null, null);
        }
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String tokenValue = jwtAuth.getToken().getTokenValue();
            if (tokenValue != null && !tokenValue.isEmpty()) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue);
            }
        }
        return headers;
    }
}