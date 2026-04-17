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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    // Chỉ định dùng Pool đã tạo ở AsyncConfig
    @Async("mailTaskExecutor")
    public void sendVerificationEmail(String toEmail, String verifyToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@googlemeetclone.com", "Kallio");
            helper.setTo(toEmail);

            helper.setSubject("Mã xác thực tài khoản Kallio của bạn");

            Context context = new Context();

            context.setVariable("verifyCode", verifyToken);

            String htmlContent = templateEngine.process("email/active-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Đã gửi email chứa mã xác thực thành công tới: {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Lỗi khi gửi email xác thực tới {}: {}", toEmail, e.getMessage());
            throw new EmailSendingException("Không thể gửi email xác thực. Vui lòng thử lại sau.", e);
        }
    }

    @Async("mailTaskExecutor")
    public void sendMeetingInvitation(String toEmail, String hostName, String meetingTitle, String meetingTime, String joinLink, String meetingCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // Đặt tham số multipart = true để gửi được HTML có định dạng phức tạp
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@googlemeetclone.com", "Kallio");
            helper.setTo(toEmail);
            helper.setSubject("Thư mời tham gia cuộc họp: " + meetingTitle);

            // Truyền dữ liệu vào Template
            Context context = new Context();
            context.setVariable("hostName", hostName);
            context.setVariable("meetingTitle", meetingTitle);
            context.setVariable("meetingTime", meetingTime);
            context.setVariable("joinLink", joinLink);
            context.setVariable("meetingCode", meetingCode);

            // Chỉ định file HTML template (nằm trong thư mục resources/templates/email/)
            String htmlContent = templateEngine.process("email/meeting-invitation", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Đã gửi email lời mời họp thành công tới: {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Lỗi khi gửi email lời mời họp tới {}: {}", toEmail, e.getMessage());
            // Có thể dùng chung exception của bạn hoặc tạo mới InvitationEmailException
            throw new EmailSendingException("Không thể gửi email lời mời. Vui lòng thử lại sau.", e);
        }
    }
}