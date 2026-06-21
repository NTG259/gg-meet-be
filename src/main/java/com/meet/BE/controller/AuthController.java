package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.dto.CreateUserDTO;
import com.meet.BE.domain.dto.LoginDTO;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.request.UpdateAccountRequest;
import com.meet.BE.domain.response.LoginResponse;
import com.meet.BE.domain.response.UserResponseDTO;
import com.meet.BE.service.user.SecurityService;
import com.meet.BE.service.user.UserService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
@Valid
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityService securityService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginDTO loginDTO,
            @RequestHeader(name = "X-Client", required = false) String clientType
    ) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());
        try {
            Authentication authentication =
                    authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            LoginResponse loginResponse = new LoginResponse();
            User currentUserDB = this.userService.handleFindUserByEmail(loginDTO.getEmail());

            if (currentUserDB != null) {
                LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                        currentUserDB.getId(),
                        currentUserDB.getEmail(),
                        currentUserDB.getFullName(),
                        currentUserDB.getAvatar(),
                        currentUserDB.getRole()
                );
                loginResponse.setUser(userLogin);
            }

            loginResponse.setAccessToken(this.securityService.createAccessToken(loginResponse));

            String refreshToken = securityService.createRefreshToken(loginResponse);
            this.userService.updateUserToken(refreshToken, loginResponse.getUser().getEmail());

            if (isTokenClient(null, clientType)) {
                loginResponse.setRefreshToken(refreshToken);
            }

            ResponseCookie refreshTokenCookie = buildRefreshTokenCookie(refreshToken);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(new ApiResponse<>(loginResponse, "Login successful", null, HttpStatus.OK.value()));

        } catch (DisabledException e) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.USER_LOCKED);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(
            @RequestBody CreateUserDTO createUserDTO) {
        ApiResponse<UserResponseDTO> response = this.userService.createUser(createUserDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/account")
    public ResponseEntity<LoginResponse.UserLogin> getAccount() {
        String email = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        User user = this.userService.handleFindUserByEmail(email);
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                user.getRole()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userLogin);
    }

    @PutMapping("/account")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateAccount(
            @Valid @RequestBody UpdateAccountRequest request) {
        String email = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        ApiResponse<UserResponseDTO> response = this.userService.updateCurrentUserAccount(email, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> getRefreshToken(
            @CookieValue(name = "rf_token", required = false) String cookieRefreshToken,
            @RequestHeader(name = "X-Refresh-Token", required = false) String headerRefreshToken,
            @RequestHeader(name = "X-Client", required = false) String clientType
    ) {
        // Non-browser clients (Electron app://, Flutter) cannot use the cookie
        // jar; they send the refresh token explicitly and get the rotated one
        // back in the body. The web keeps the cookie-only flow untouched.
        String refreshToken = (headerRefreshToken != null && !headerRefreshToken.isBlank())
                ? headerRefreshToken
                : cookieRefreshToken;

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Jwt decodedToken = this.securityUtils.checkValidRefreshToken(refreshToken);
        String email = decodedToken.getSubject();

        User user = this.userService.getUserByEmailAndRefreshToken(email, refreshToken);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        LoginResponse loginResponse = new LoginResponse();
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                user.getId(), user.getEmail(), user.getFullName(), user.getAvatar(), user.getRole()
        );
        loginResponse.setUser(userLogin);
        loginResponse.setAccessToken(this.securityService.createAccessToken(loginResponse));

        String newRefreshToken = securityService.createRefreshToken(loginResponse);
        this.userService.updateUserToken(newRefreshToken, email);

        if (isTokenClient(headerRefreshToken, clientType)) {
            loginResponse.setRefreshToken(newRefreshToken);
        }

        ResponseCookie refreshTokenCookie = buildRefreshTokenCookie(newRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(new ApiResponse<>(loginResponse, "Token refreshed successfully", null, HttpStatus.OK.value()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        String email = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        userService.revokeUserTokens(email);

        ResponseCookie deleteRefreshTokenCookie = ResponseCookie
                .from("rf_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString())
                .build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<?>> verifyEmail(@RequestParam("token") String token) {
        try {
            securityService.verifyEmailToken(token);
            return ResponseEntity.status(HttpStatus.FOUND).
                    body(
                            new ApiResponse<>(
                                    null,
                                    "Email verification succeeded",
                                    null,
                                    HttpStatus.FOUND.value()
                            )
                    );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).
                    body(
                            new ApiResponse<>(
                                    null,
                                    "Email verification failed",
                                    null,
                                    HttpStatus.NOT_FOUND.value()
                            )
                    );
        }
    }

    @PostMapping("/send-verify-code")
    public ResponseEntity<ApiResponse<?>> sendVerifyCode(@RequestParam("email") String email) {
        securityService.sendVerifyCode(email);
        return ResponseEntity.status(HttpStatus.OK).
                body(
                        new ApiResponse<>(
                                null,
                                "Verification code sent to " + email,
                                null,
                                HttpStatus.OK.value()
                        )
                );

    }

    private boolean isTokenClient(String headerRefreshToken, String clientType) {
        if (headerRefreshToken != null && !headerRefreshToken.isBlank()) {
            return true;
        }

        return "desktop".equalsIgnoreCase(clientType) || "mobile".equalsIgnoreCase(clientType);
    }

    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie
                .from("rf_token", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();
    }
}
