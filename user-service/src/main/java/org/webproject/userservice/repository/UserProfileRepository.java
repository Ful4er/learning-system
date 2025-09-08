package org.webproject.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.webproject.userservice.model.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile,Long> {
}
