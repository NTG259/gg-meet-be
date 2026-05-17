package com.meet.BE.service.user;

import com.meet.BE.utils.errors.EmailSendingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.from-name:Kallio}")
    private String mailFromName;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Async("mailTaskExecutor")
    public void sendVerificationEmail(String toEmail, String verifyToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom, mailFromName);
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
    public void sendMeetingInvitation(
            String toEmail,
            String hostName,
            String meetingTitle,
            String meetingTime,
            String meetingCode
    ) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skip sending email because address is invalid");
            return;
        }

        try {
            String joinLink = buildJoinLink(meetingCode);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(toEmail);
            helper.setSubject("Thư mời tham gia cuộc họp: " + meetingTitle);

            String htmlContent = buildInvitationTemplate(
                    hostName,
                    meetingTitle,
                    meetingTime,
                    joinLink,
                    meetingCode
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Sent meeting invitation to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildJoinLink(String meetingCode) {
        return frontendBaseUrl + "/" + meetingCode;
    }

    private String buildInvitationTemplate(
            String hostName,
            String meetingTitle,
            String meetingTime,
            String joinLink,
            String meetingCode
    ) {
        Context context = new Context();
        context.setVariable("hostName", hostName);
        context.setVariable("meetingTitle", meetingTitle);
        context.setVariable("meetingTime", meetingTime);
        context.setVariable("joinLink", joinLink);
        context.setVariable("meetingCode", meetingCode);

        return templateEngine.process("email/meeting-invitation", context);
    }
}
