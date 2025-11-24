package org.webproject.userservice.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.webproject.userservice.model.User;
import org.webproject.userservice.repository.UserRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultAdmin() {
        String adminEmail = "admin@example.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setCreatedAt(LocalDateTime.now());

            userRepository.save(admin);

            System.out.println("Default admin created: admin@example.com / admin123");
        }

        String teacherEmail = "teacher1@example.com";
        if (!userRepository.existsByEmail(teacherEmail)) {
            User teacher = new User();
            teacher.setId(2L);
            teacher.setFirstName("John");
            teacher.setLastName("Teacher");
            teacher.setEmail(teacherEmail);
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setRole(Role.TEACHER);
            teacher.setCreatedAt(LocalDateTime.now());
            try {
                userRepository.save(teacher);
                System.out.println("Sample teacher created: " + teacherEmail + " / teacher123 (id=" + teacher.getId() + ")");
            } catch (Exception ex) {
                System.out.println("Failed to create sample teacher with fixed id, saving without id fallback: " + ex.getMessage());
                teacher.setId(null);
                userRepository.save(teacher);
                System.out.println("Sample teacher created (fallback): " + teacherEmail + " / teacher123 (id=" + teacher.getId() + ")");
            }
        }

        String[] studentEmails = new String[]{"student1@example.com", "student2@example.com", "student3@example.com"};
        for (int i = 0; i < studentEmails.length; i++) {
            String em = studentEmails[i];
            if (!userRepository.existsByEmail(em)) {
                User s = new User();
                s.setId(3L + i);
                s.setFirstName("Student" + (i + 1));
                s.setLastName("Test");
                s.setEmail(em);
                s.setPassword(passwordEncoder.encode("student123"));
                s.setRole(Role.STUDENT);
                s.setCreatedAt(LocalDateTime.now());
                try {
                    userRepository.save(s);
                    System.out.println("Sample student created: " + em + " / student123 (id=" + s.getId() + ")");
                } catch (Exception ex) {
                    System.out.println("Failed to create sample student with fixed id, saving without id fallback: " + ex.getMessage());
                    s.setId(null);
                    userRepository.save(s);
                    System.out.println("Sample student created (fallback): " + em + " / student123 (id=" + s.getId() + ")");
                }
            }
        }
    }
}