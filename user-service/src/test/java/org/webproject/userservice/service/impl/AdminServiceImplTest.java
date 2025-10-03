package org.webproject.userservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webproject.userservice.exception.UserNotFoundException;
import org.webproject.userservice.model.User;
import org.webproject.userservice.repository.UserRepository;
import org.webproject.userservice.util.Role;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getAllUsers_ReturnsList() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(new User(), new User()));

        List<User> users = adminService.getAllUsers();

        assertEquals(2, users.size());
        verify(userRepository).findAll();
    }

    @Test
    void updateUserRole_Success() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setRole(Role.STUDENT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = adminService.updateUserRole(userId, Role.TEACHER);

        assertEquals(Role.TEACHER, updated.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserRole_UserNotFound_Throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> adminService.updateUserRole(999L, Role.TEACHER));
    }

    @Test
    void deleteUser_Success() {
        Long userId = 2L;
        when(userRepository.existsById(userId)).thenReturn(true);

        adminService.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_NotFound_Throws() {
        when(userRepository.existsById(404L)).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> adminService.deleteUser(404L));
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void getUsersByRole_ReturnsList() {
        when(userRepository.findByRole(Role.TEACHER)).thenReturn(List.of(new User()));

        List<User> teachers = adminService.getUsersByRole(Role.TEACHER);

        assertEquals(1, teachers.size());
        verify(userRepository).findByRole(Role.TEACHER);
    }
}


