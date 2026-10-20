package com.meet.BE.service.user;

import com.meet.BE.domain.entity.VerificationToken;
import com.meet.BE.repository.TokenRepository;
import com.meet.BE.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock private JwtEncoder jwtEncoder;
    @Mock private TokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @InjectMocks private SecurityService securityService;

    @Test
    void resendingVerificationCodeRenewsExpiredToken() {
        String email = "new-user@example.com";
        VerificationToken token = new VerificationToken("123456", email);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByEmail(email)).thenReturn(Optional.of(token));

        LocalDateTime beforeSend = LocalDateTime.now();
        securityService.sendVerifyCode(email);

        assertThat(token.getExpiryDate()).isAfter(beforeSend.plusMinutes(9));
        assertThat(token.getToken()).matches("\\d{6}");
        verify(tokenRepository).save(token);
        verify(emailService).sendVerificationEmail(eq(email), eq(token.getToken()));
    }
}
