package com.meet.BE.service;

import com.meet.BE.utils.errors.EmailSendingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Chỉ định dùng Pool đã tạo ở AsyncConfig
    @Async("mailTaskExecutor")
    public void sendVerificationEmail(String toEmail, String confirmationUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Có tên ứng dụng rõ ràng, tránh spam
            helper.setFrom("noreply@googlemeetclone.com", "Google Meet Clone");
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản của bạn");

            // HTML Template (Tương lai có thể dùng Thymeleaf ở đây)
            String htmlContent = "<div style='font-family: Arial, sans-serif; text-align: center;'>"
                    + "<h2>Chào mừng bạn đến với hệ thống!</h2>"
                    + "<p>Vui lòng click vào nút bên dưới để kích hoạt tài khoản:</p>"
                    + "<a href='" + confirmationUrl + "' style='padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>Xác Thực Tài Khoản</a>"
                    + "<p style='color: #777; margin-top: 20px;'>Link này sẽ hết hạn sau 15 phút.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Đã gửi email xác thực thành công tới: {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Lỗi khi gửi email xác thực tới {}: {}", toEmail, e.getMessage());
            // Ném Custom Exception để Controller Advice bắt lại (nếu cần)
            throw new EmailSendingException("Không thể gửi email xác thực. Vui lòng thử lại sau.", e);
        }
    }
}