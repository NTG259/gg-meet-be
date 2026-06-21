package com.meet.BE.controller;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.dto.LoginDTO;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.response.LoginResponse;
import com.meet.BE.service.user.SecurityService;
import com.meet.BE.service.user.UserService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import com.meet.BE.utils.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String EMAIL = "user@example.com";

    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Mock
    private SecurityService securityService;

    @Mock
    private UserService userService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AuthController authController;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "refreshTokenExpiration", 3600L);

        user = new User();
        user.setEmail(EMAIL);
        user.setFullName("Test User");
    }

    private Jwt refreshJwt() {
        return Jwt.withTokenValue("old-refresh")
                .header("alg", "HS256")
                .subject(EMAIL)
                .build();
    }

    private void stubLogin() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userService.handleFindUserByEmail(EMAIL)).thenReturn(user);
        when(securityService.createAccessToken(any(LoginResponse.class))).thenReturn("new-access");
        when(securityService.createRefreshToken(any(LoginResponse.class))).thenReturn("new-refresh");
    }

    private void stubRefresh(String presentedToken) {
        lenient().when(securityUtils.checkValidRefreshToken(presentedToken)).thenReturn(refreshJwt());
        lenient().when(userService.getUserByEmailAndRefreshToken(EMAIL, presentedToken)).thenReturn(user);
        lenient().when(securityService.createAccessToken(any(LoginResponse.class))).thenReturn("new-access");
        lenient().when(securityService.createRefreshToken(any(LoginResponse.class))).thenReturn("new-refresh");
    }

    @Test
    void refreshWithHeaderTokenReturnsRotatedRefreshTokenInBody() {
        stubRefresh("old-refresh");

        ResponseEntity<ApiResponse<LoginResponse>> response =
                authController.getRefreshToken(null, "old-refresh", "desktop");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse body = response.getBody().data();
        assertThat(body.getAccessToken()).isEqualTo("new-access");
        assertThat(body.getRefreshToken()).isEqualTo("new-refresh");
        // The cookie is still set; harmless for token clients (credentials omitted).
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("new-refresh");
    }

    @Test
    void refreshWithHeaderTokenWithoutClientHeaderStillReturnsBodyToken() {
        stubRefresh("old-refresh");

        ResponseEntity<ApiResponse<LoginResponse>> response =
                authController.getRefreshToken(null, "old-refresh", null);

        assertThat(response.getBody().data().getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refreshWithCookieOnlyKeepsRefreshTokenOutOfBody() {
        stubRefresh("old-refresh");

        ResponseEntity<ApiResponse<LoginResponse>> response =
                authController.getRefreshToken("old-refresh", null, null);

        LoginResponse body = response.getBody().data();
        assertThat(body.getAccessToken()).isEqualTo("new-access");
        assertThat(body.getRefreshToken()).isNull();
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("new-refresh");
    }

    @Test
    void refreshWithoutAnyTokenThrowsUnauthorizedInsteadOfBindingError() {
        assertThatThrownBy(() -> authController.getRefreshToken(null, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void loginWithDesktopClientReturnsRefreshTokenInBody() {
        stubLogin();

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail(EMAIL);
        loginDTO.setPassword("secret");

        ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(loginDTO, "desktop");

        LoginResponse body = response.getBody().data();
        assertThat(body.getAccessToken()).isEqualTo("new-access");
        assertThat(body.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void loginWithoutClientHeaderKeepsRefreshTokenOutOfBody() {
        stubLogin();

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail(EMAIL);
        loginDTO.setPassword("secret");

        ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(loginDTO, null);

        LoginResponse body = response.getBody().data();
        assertThat(body.getAccessToken()).isEqualTo("new-access");
        assertThat(body.getRefreshToken()).isNull();
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("new-refresh");
    }
}
