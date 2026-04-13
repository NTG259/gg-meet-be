package com.meet.BE.controller.auth;


import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.dto.CreateUserDTO;
import com.meet.BE.domain.dto.LoginDTO;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.response.LoginResponse;
import com.meet.BE.domain.response.UserResponseDTO;
import com.meet.BE.service.SecurityService;
import com.meet.BE.service.UserService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
@Valid
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityService securityService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    @Value("${jwt.expiration}")
    private Long expiration;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
        try {
            Authentication authentication =
                    authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            //create token
            LoginResponse loginResponse = new LoginResponse();

            User currentUserDB = this.userService.handleFindUserByEmail(loginDTO.getUsername());
            if(currentUserDB != null) {
                LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                        currentUserDB.getId(),
                        currentUserDB.getEmail(),
                        currentUserDB.getFullName(),
                        currentUserDB.getRole()
                );
                loginResponse.setUser(userLogin);
            }
            String accessToken = this.securityService.createAccessToken(loginResponse);
            loginResponse.setAccessToken(accessToken);

            ResponseCookie cookie = ResponseCookie
                    .from("access_token", accessToken)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(expiration)
                    .build();



            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new ApiResponse<>(loginResponse, "Đăng nhập thành công", null, HttpStatus.OK.value()));
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

    // User F5
    @GetMapping("/account")
    public ResponseEntity<LoginResponse.UserLogin> getAccount() {
        String email = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        User user = this.userService.handleFindUserByEmail(email);
        LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userLogin);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {

        String email = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        userService.updateUserToken(null, email);

        ResponseCookie deleteCookie = ResponseCookie
                .from("access_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }
}