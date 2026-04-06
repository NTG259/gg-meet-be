package com.meet.BE.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.meet.BE.domain.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthService {

    // Lấy Client ID từ file cấu hình
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    public User verifyGoogleToken(String idTokenString) {
        try {
            // Khởi tạo bộ xác thực của Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            // Yêu cầu Google kiểm tra tính hợp lệ của token
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                // Lấy các thông tin định danh
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String providerId = payload.getSubject(); // ID duy nhất của user này trên hệ thống Google

                // TODO: Gọi đến UserRepository để kiểm tra xem email/providerId này đã có trong DB chưa.
                // Nếu chưa có =<> Tạo mới User và lưu xuống DB.
                // Nếu đã có =<> Cập nhật thông tin (nếu cần) và lấy User đó ra.

                // Trả về object User (Entity hoặc DTO của bạn)
                User user = new User();
                user.setEmail(email);
                user.setFullName(name);
                user.setAvatarUrl(pictureUrl);
                user.setProvider("GOOGLE");
                user.setProviderId(providerId);

                return user;
            } else {
                throw new RuntimeException("Token Google gửi lên không hợp lệ hoặc đã hết hạn!");
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xác thực với Google: " + e.getMessage());
        }
    }
}