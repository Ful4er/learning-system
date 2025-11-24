package org.webproject.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.webproject.userservice.model.User;
import org.webproject.userservice.util.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.id = :userId")
    Optional<User> findByIdWithProfile(@Param("userId") Long userId);
    List<User> findTop10ByEmailIgnoreCaseContainingAndRole(String email, Role role);
    List<User> findTop10ByFirstNameIgnoreCaseContainingOrLastNameIgnoreCaseContainingAndRole(String firstName, String lastName, Role role);
}