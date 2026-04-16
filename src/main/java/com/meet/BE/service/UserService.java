package com.meet.BE.service;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.dto.CreateUserDTO;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    public ApiResponse<UserResponseDTO> createUser(CreateUserDTO userDto) {
        if (userRepository.findByEmailAndEnabled(userDto.getEmail(), true).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXIST);
        }

        User newUser = new User();
        newUser.setEmail(userDto.getEmail());
        newUser.setFullName(userDto.getFullName());
        newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        newUser = this.userRepository.save(newUser);

        String verifyToken = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(verifyToken, newUser);
        tokenRepository.save(verificationToken);

        // 3. Gửi Mail
        emailService.sendVerificationEmail(newUser.getEmail(), verifyToken);
        return new ApiResponse<>(
                UserConvert.convertToUserResponseDTO(newUser),
                "Đăng ký thành công. Vui lòng kiểm tra email để xác thực.",
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

    public User getUserByEmailAndRefreshToken(String email, String refreshToken) {
        return this.userRepository.findUserByEmailAndRefreshToken(email, refreshToken)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.UNAUTHORIZED)
                );
    }
}
