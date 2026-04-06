package com.meet.BE.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.security.AuthProvider;


@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String fullName;
    private String avatarUrl;

    // Phân biệt user tự tạo tài khoản (LOCAL) hay đăng nhập bằng Google (GOOGLE)
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    // ID định danh của Google trả về
    private String providerId;

    // Mật khẩu có thể null nếu đăng nhập bằng Google
    private String password;

    // ... các trường khác (role, created_at...)
}