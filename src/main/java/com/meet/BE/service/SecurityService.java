package com.meet.BE.service;

import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.entity.VerificationToken;
import com.meet.BE.domain.response.LoginResponse;
import com.meet.BE.repository.TokenRepository;
import com.meet.BE.repository.UserRepository;
import com.meet.BE.utils.errors.AuthException;
import com.meet.BE.utils.errors.ErrorCode;
import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecurityService {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;
    private final JwtEncoder jwtEncoder;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String jwtKey;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityService.JWT_ALGORITHM.getName());
    }

    public String createAccessToken(LoginResponse loginResponse) {
        LoginResponse.UserInsideToken userToken = new LoginResponse.UserInsideToken();
        userToken.setId(loginResponse.getUser().getId());
        userToken.setFullName(loginResponse.getUser().getFullName());
        userToken.setEmail(loginResponse.getUser().getEmail());
        userToken.setRole(loginResponse.getUser().getRole());
        Instant now = Instant.now();
        // Sửa lại dùng đúng accessTokenExpiration
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);

        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(loginResponse.getUser().getEmail())
                .claim("user", userToken)
                .claim("roles", userToken.getRole())
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String createRefreshToken(LoginResponse loginResponse) {
        LoginResponse.UserInsideToken userToken = new LoginResponse.UserInsideToken();
        userToken.setId(loginResponse.getUser().getId());
        userToken.setFullName(loginResponse.getUser().getFullName());
        userToken.setEmail(loginResponse.getUser().getEmail());
        userToken.setRole(loginResponse.getUser().getRole());
        Instant now = Instant.now();
        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);

        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(userToken.getEmail())
                .claim("roles", userToken.getRole())
                .claim("user", userToken)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    // =========================================================================
    // 1. TẠO VÉ MEETING (Dùng JwtEncoder của Spring Security)
    // =========================================================================
    public String generateMeetingToken(Long participantId, String meetingCode, String role) {
        Instant now = Instant.now();
        // Hạn sử dụng vé: 12 tiếng
        Instant validity = now.plus(12, ChronoUnit.HOURS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(String.valueOf(participantId))
                .claim("participantId", participantId) // Lưu ID của người tham gia
                .claim("meetingCode", meetingCode)     // Lưu mã phòng họp
                .claim("role", role)                   // Lưu quyền (HOST/PARTICIPANT)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    // =========================================================================
    // 2. GIẢI MÃ VÉ MEETING (Dùng NimbusJwtDecoder)
    // =========================================================================
    public Long getParticipantIdFromToken(String token) {
        // Tạo Decoder từ chính Secret Key của bạn
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                .macAlgorithm(JWT_ALGORITHM)
                .build();

        // Giải mã token
        Jwt jwt = jwtDecoder.decode(token);

        // Lấy trường participantId ra và parse về Long
        Object participantIdObj = jwt.getClaim("participantId");
        return Long.parseLong(participantIdObj.toString());
    }

    @Transactional
    public void verifyEmailToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_TOKEN));

        // Kiểm tra hết hạn
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            // Xóa token cũ rác đi để tối ưu DB
            tokenRepository.delete(verificationToken);
            throw new AuthException(ErrorCode.EXPIRED_TOKEN);
        }

        // Kích hoạt User
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // Xóa token sau khi sử dụng thành công (Best practice)
        tokenRepository.delete(verificationToken);
    }
}