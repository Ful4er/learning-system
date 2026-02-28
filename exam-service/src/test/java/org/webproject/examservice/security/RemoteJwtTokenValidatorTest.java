package org.webproject.examservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RemoteJwtTokenValidatorTest {

    static class StubTokenValidationService implements TokenValidationService {
        boolean active;
        String lastToken;

        @Override
        public boolean isTokenActive(String token) {
            this.lastToken = token;
            return active;
        }
    }

    private StubTokenValidationService stubService;
    private RemoteJwtTokenValidator validator;

    @BeforeEach
    void setUp() {
        stubService = new StubTokenValidationService();
        validator = new RemoteJwtTokenValidator(stubService);
    }

    private Jwt createJwt(String tokenValue) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("sub", "user")
                .build();
    }

    @Test
    void testValidate_WhenTokenIsActive_ReturnsSuccess() {
        String tokenValue = "test-token";
        stubService.active = true;
        Jwt jwt = createJwt(tokenValue);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertFalse(result.hasErrors());
        assertEquals(tokenValue, stubService.lastToken);
    }

    @Test
    void testValidate_WhenTokenIsInactive_ReturnsFailure() {
        String tokenValue = "test-token";
        stubService.active = false;
        Jwt jwt = createJwt(tokenValue);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertTrue(result.hasErrors());
        OAuth2Error error = result.getErrors().iterator().next();
        assertEquals("inactive_token", error.getErrorCode());
        assertEquals("JWT token is inactive according to user-service", error.getDescription());
    }

    @Test
    void testValidate_ExtractsTokenValueCorrectly() {
        String tokenValue = "bearer-token-123";
        stubService.active = true;
        Jwt jwt = createJwt(tokenValue);

        validator.validate(jwt);

        assertEquals(tokenValue, stubService.lastToken);
    }
}