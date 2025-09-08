package org.webproject.userservice.service;

import org.webproject.userservice.model.User;
import org.webproject.userservice.util.Role;

import java.util.List;

public interface AdminService {
    List<User> getAllUsers();
    User updateUserRole(Long userId, Role newRole);
    void deleteUser(Long userId);
    List<User> getUsersByRole(Role role);
}
