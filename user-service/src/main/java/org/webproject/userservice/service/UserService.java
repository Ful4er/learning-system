package org.webproject.userservice.service;

import org.springframework.transaction.annotation.Transactional;
import org.webproject.userservice.dto.request.RegisterRequest;
import org.webproject.userservice.model.User;
import org.webproject.userservice.util.Role;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<User> getUserById(Long userId);
    User updateUser(User user);
    User createUser(User user, Role role);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    void updateLastLogin(Long userId);
}