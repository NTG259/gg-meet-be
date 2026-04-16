package com.meet.BE.domain.entity;

import com.meet.BE.domain.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = true)
    private String password;

    // Phân biệt user tự tạo tài khoản (LOCAL) hay đăng nhập bằng Google, GitHub
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'LOCAL'")
    private AuthProvider authProvider;

    // ID định danh của OAuth provider (Google, GitHub, ...)
    @Column(nullable = true)
    private String providerId;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean enabled;

    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'USER'")
    private String role;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String refreshToken;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public User() {
        this.authProvider = AuthProvider.LOCAL;
        this.enabled = false;
        this.role = "USER";
    }
}