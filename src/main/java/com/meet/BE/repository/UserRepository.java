package com.meet.BE.repository;

import com.meet.BE.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findUserByEmailAndRefreshToken(String email, String rf);
    Optional<User> findByEmailAndEnabled(String email, Boolean isEnabled);

    boolean existsByEmail(String email);
}
