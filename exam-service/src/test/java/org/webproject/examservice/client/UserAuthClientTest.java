package org.webproject.examservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.webproject.examservice.client.UserDtoMapper;
import org.webproject.examservice.dto.response.TokenIntrospectionResponse;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private UserServiceClient userServiceClient;
    private UserDtoMapper userDtoMapper;

    @BeforeEach
    void setUp() throws Exception {
        userDtoMapper = new UserDtoMapper();
        userServiceClient = new UserServiceClient(webClient, new ObjectMapper(), userDtoMapper);

        setFieldValue(userServiceClient, "authIntrospectionUrl", "http://localhost:8080/api/auth/introspect");
    }

    private void setFieldValue(Object target, String fieldName, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testIntrospectToken_WhenSuccessful_ReturnsResponse() {
        String token = "test-token";
        long exp = Instant.now().plusSeconds(3600).getEpochSecond();
        TokenIntrospectionResponse expectedResponse = new TokenIntrospectionResponse(
                true, 1L, "STUDENT", "user@test.com", "user@test.com", exp
        );

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(TokenIntrospectionResponse.class)))
                .thenReturn(Mono.just(expectedResponse));

        TokenIntrospectionResponse result = userServiceClient.introspectToken(token);

        assertNotNull(result);
        assertTrue(result.isActive());
        assertEquals(1L, result.getUserId());
    }

    @Test
    void testIntrospectToken_WhenEmptyResponse_ReturnsInactiveResponse() {
        String token = "test-token";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(TokenIntrospectionResponse.class)))
                .thenReturn(Mono.empty());

        TokenIntrospectionResponse result = userServiceClient.introspectToken(token);

        assertNotNull(result);
        assertFalse(result.isActive());
    }

    @Test
    void testIntrospectToken_WhenWebClientException_ReturnsInactiveResponse() {
        String token = "test-token";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(TokenIntrospectionResponse.class)))
                .thenReturn(Mono.error(new WebClientResponseException(401, "Unauthorized", null, null, null)));

        TokenIntrospectionResponse result = userServiceClient.introspectToken(token);

        assertNotNull(result);
        assertFalse(result.isActive());
    }

    @Test
    void testIntrospectToken_WhenGeneralException_ReturnsInactiveResponse() {
        String token = "test-token";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(TokenIntrospectionResponse.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection error")));

        Logger logger = (Logger) LoggerFactory.getLogger(UserServiceClient.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        TokenIntrospectionResponse result;
        try {
            result = userServiceClient.introspectToken(token);
        } finally {
            logger.setLevel(previousLevel);
        }

        assertNotNull(result);
        assertFalse(result.isActive());
    }
}