package com.meet.BE.service.user;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.dto.CreateUserDTO;
import com.meet.BE.domain.request.UpdateAccountRequest;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.entity.VerificationToken;
import com.meet.BE.domain.response.UserResponseDTO;
import com.meet.BE.repository.TokenRepository;
import com.meet.BE.repository.UserRepository;
import com.meet.BE.utils.convert.UserConvert;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    @Transactional
    public ApiResponse<UserResponseDTO> createUser(CreateUserDTO userDto) {
        if (userRepository.findByEmailAndEnabled(userDto.getEmail(), true).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXIST);
        }

        User newUser = new User();
        newUser.setEmail(userDto.getEmail());
        newUser.setFullName(userDto.getFullName());
        newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));


        VerificationToken token = tokenRepository.findByToken(userDto.getVerifyCode()).orElseThrow(
                () -> new BusinessException(ErrorCode.INVALID_TOKEN)
        );

        if (!Objects.equals(token.getEmail(), userDto.getEmail()) || token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        tokenRepository.delete(token);
        newUser.setEnabled(true);
        newUser = this.userRepository.save(newUser);
        return new ApiResponse<>(
                UserConvert.convertToUserResponseDTO(newUser),
                "Đăng ký thành công.",
                null,
                HttpStatus.CREATED.value());
    }

    public ApiResponse<UserResponseDTO> getUserById(Long id) {
        User user = this.userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new ApiResponse<>(
                UserConvert.convertToUserResponseDTO(user),
                "Lấy thông tin thành công",
                null,
                HttpStatus.OK.value());
    }


    public User handleFindUserByEmail(String email) {
        return this.userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public void updateUserToken(String token, String email) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.setRefreshToken(token);
        this.userRepository.save(user);
    }

    @Transactional
    public void revokeUserTokens(String email) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Long currentVersion = user.getAccessTokenVersion() == null ? 0L : user.getAccessTokenVersion();
        user.setAccessTokenVersion(currentVersion + 1);
        user.setRefreshToken(null);
        this.userRepository.save(user);
    }

    @Transactional
    public ApiResponse<UserResponseDTO> updateCurrentUserAccount(String email, UpdateAccountRequest request) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setFullName(request.getFullName().trim());
        user.setAvatar(request.getAvatar() == null || request.getAvatar().isBlank()
                ? null
                : request.getAvatar().trim());

        User updatedUser = this.userRepository.save(user);
        return new ApiResponse<>(
                UserConvert.convertToUserResponseDTO(updatedUser),
                "Account updated successfully",
                null,
                HttpStatus.OK.value()
        );
    }

    public User getUserByEmailAndRefreshToken(String email, String refreshToken) {
        return this.userRepository.findUserByEmailAndRefreshToken(email, refreshToken)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.UNAUTHORIZED)
                );
    }
}
