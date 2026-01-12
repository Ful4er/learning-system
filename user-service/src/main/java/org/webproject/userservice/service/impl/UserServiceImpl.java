package org.webproject.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webproject.userservice.dto.request.RegisterRequest;
import org.webproject.userservice.exception.EmailAlreadyExistsException;
import org.webproject.userservice.model.User;
import org.webproject.userservice.model.UserProfile;
import org.webproject.userservice.repository.UserProfileRepository;
import org.webproject.userservice.repository.UserRepository;
import org.webproject.userservice.service.UserService;
import org.webproject.userservice.util.Role;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.webproject.userservice.util.Role.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(User user, Role role) {
        user.setRole(role);
        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        userProfileRepository.save(profile);

        log.debug("Creating new user, clearing users cache");
        return savedUser;
    }

    @Override
    @Cacheable(value = "users", key="#userId")
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long userId) {
        log.debug("Fetching user from database: {}", userId);
        return userRepository.findById(userId);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#user.id"),
                    @CacheEvict(value = "users", key = "'email:' + #user.email"),
                    @CacheEvict(value = "user-current", key = "#user.id")
            }
    )
    public User updateUser(User user) {
        log.debug("Updating user: {}, evicting cache", user.getId());
        return userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Cacheable(value = "users", key = "'email:' + #email")
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Fetching user from database by email: {}", email);
        return userRepository.findByEmail(email);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#userId"),
                    @CacheEvict(value = "user-current", key = "#userId")
            }
    )
    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            var cache = cacheManager.getCache("users");
            if (cache != null && user.getEmail() != null) {
                cache.evict("email:" + user.getEmail());
            }
            log.debug("Updated last login for user: {}, evicting cache", userId);
        });
    }
    @Override
    public User createUserFromRegistration(RegisterRequest request, Role role) {
        if (existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);

        return createUser(user, role);
    }
    @Override
    public List<User> searchStudentsByEmail(String emailPart) {
        if (emailPart == null) {
            log.info("searchStudentsByEmail called with null emailPart");
            return Collections.emptyList();
        }
        String sanitized = emailPart.replaceAll("\\p{C}", "").trim();
        log.info("searchStudentsByEmail called with emailPart='{}' sanitized='{}'", emailPart, sanitized);
        return userRepository.findTop10ByEmailIgnoreCaseContainingAndRole(sanitized, STUDENT);
    }

    @Override
    public List<User> searchStudentsByName(String namePart) {
        return userRepository.findTop10ByFirstNameIgnoreCaseContainingOrLastNameIgnoreCaseContainingAndRole(namePart, namePart, STUDENT);
    }
}