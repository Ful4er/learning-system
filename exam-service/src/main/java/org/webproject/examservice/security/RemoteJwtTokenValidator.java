package org.webproject.examservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RemoteJwtTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenValidationService tokenValidationService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tokenValue = token.getTokenValue();

        if (!tokenValidationService.isTokenActive(tokenValue)) {
            OAuth2Error error = new OAuth2Error(
                    "inactive_token",
                    "JWT token is inactive according to user-service",
                    null
            );
            log.debug("JWT validation failed by remote validator: {}", error.getDescription());
            return OAuth2TokenValidatorResult.failure(error);
        }

        return OAuth2TokenValidatorResult.success();
    }
}