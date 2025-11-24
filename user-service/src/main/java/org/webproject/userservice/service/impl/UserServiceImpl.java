package org.webproject.userservice.service.impl;

import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Optional;

import static org.webproject.userservice.util.Role.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(User user, Role role) {
        user.setRole(role);
        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
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
        return userRepository.findTop10ByEmailIgnoreCaseContainingAndRole(emailPart, STUDENT);
    }

    @Override
    public List<User> searchStudentsByName(String namePart) {
        return userRepository.findTop10ByFirstNameIgnoreCaseContainingOrLastNameIgnoreCaseContainingAndRole(namePart, namePart, STUDENT);
    }
}