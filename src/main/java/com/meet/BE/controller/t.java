package com.meet.BE.controller;

import com.meet.BE.domain.entity.User;
import com.meet.BE.service.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final JwtService jwtService; // Service tự viết để tạo JWT cho hệ thống của bạn

    @PostMapping("/google")
    public ResponseEntity<Object> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        // 1. Xác thực với Google và lấy/tạo User trong Database
        User user = googleAuthService.verifyGoogleToken(request.getCredential());

        // 2. Tạo JWT nội bộ của hệ thống (App Token) cho user này
        String appToken = jwtService.generateToken(user);

        // 3. Trả về cho Next.js
        return ResponseEntity.ok(new AuthResponse(appToken, user));
    }
}
