package com.meet.BE.repository;

import com.meet.BE.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findUserByEmailAndRefreshToken(String email, String rf);
    Optional<User> findByEmailAndEnabled(String email, Boolean isEnabled);

    boolean existsByEmail(String email);

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime from,
            LocalDateTime to
    );

    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    @Query("""
            select u
            from User u
            where u.isDeleted = false
              and (:search is null or lower(u.fullName) like lower(concat('%', :search, '%')) or lower(u.email) like lower(concat('%', :search, '%')))
              and (:role is null or u.role = :role)
              and (:enabled is null or u.enabled = :enabled)
            """)
    Page<User> findForAdmin(
            @Param("search") String search,
            @Param("role") String role,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN' AND u.isDeleted = false AND u.enabled = true AND u.isBanned = false")
    long countActiveAdmins();
}
