package org.webproject.examservice.security;

public interface TokenValidationService {
    boolean isTokenActive(String token);
}