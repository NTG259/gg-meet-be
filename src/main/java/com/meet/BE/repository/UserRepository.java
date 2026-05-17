package com.meet.BE.repository;

import com.meet.BE.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findUserByEmailAndRefreshToken(String email, String rf);
    Optional<User> findByEmailAndEnabled(String email, Boolean isEnabled);

    boolean existsByEmail(String email);

    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
