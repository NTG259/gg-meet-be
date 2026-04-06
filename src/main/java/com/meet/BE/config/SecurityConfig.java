package com.meet.BE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    // Inject các dependency cần thiết: UserRepository, JwtService, JwtAuthenticationFilter...

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF vì chúng ta dùng JWT (Stateless)
                .csrf(csrf -> csrf.disable())

                // 2. Chuyển Session Management sang Stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Phân quyền Endpoint
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/login/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 4. Cấu hình OAuth2
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                            String email = oAuth2User.getAttribute("email");
                            String name = oAuth2User.getAttribute("name");

                            // TODO Bước 1: Lưu User vào DB
                            // TODO Bước 2: Tạo JWT Token
                            String mockToken = "eyJhbGciOiJIUzI1Ni... (JWT thực tế)";

                            // TODO Bước 3: Redirect về Frontend
                            response.sendRedirect("http://localhost:3000/oauth2/redirect?token=" + mockToken);
                        })
                );

        // 5. Thêm JwtAuthenticationFilter vào trước UsernamePasswordAuthenticationFilter
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}