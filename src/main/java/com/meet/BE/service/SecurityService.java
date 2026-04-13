package com.meet.BE.service;


import com.meet.BE.domain.response.LoginResponse;
import com.nimbusds.jose.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Service
@RequiredArgsConstructor
public class SecurityService {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.secret}")
    private String jwtKey;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityService.JWT_ALGORITHM.getName());
    }

    public String createAccessToken(LoginResponse loginResponse) {
        LoginResponse.UserInsideToken userToken = new LoginResponse.UserInsideToken();
        userToken.setId(loginResponse.getUser().getId());
        userToken.setName(loginResponse.getUser().getName());
        userToken.setEmail(loginResponse.getUser().getEmail());
        userToken.setRole(loginResponse.getUser().getRole());
        Instant now = Instant.now();
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

}