package org.webproject.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.webproject.userservice.exception.UserNotFoundException;
import org.webproject.userservice.model.User;
import org.webproject.userservice.repository.UserRepository;
import org.webproject.userservice.service.AdminService;
import org.webproject.userservice.util.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#userId"),
                    @CacheEvict(value = "user-current", key = "#userId")
            }
    )
    public User updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setRole(newRole);
        User savedUser = userRepository.save(user);

        var cache = cacheManager.getCache("users");
        if (cache != null && savedUser.getEmail() != null) {
            cache.evict("email:" + savedUser.getEmail());
        }
        
        return savedUser;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = "users", key = "#userId"),
                    @CacheEvict(value = "user-current", key = "#userId")
            }
    )
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        userRepository.deleteById(userId);

        var cache = cacheManager.getCache("users");
        if (cache != null && user.getEmail() != null) {
            cache.evict("email:" + user.getEmail());
        }
    }

    @Override
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }
}
