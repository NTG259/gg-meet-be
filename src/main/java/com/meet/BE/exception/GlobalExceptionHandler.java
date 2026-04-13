package com.meet.BE.exception;

import com.meet.BE.domain.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý AuthenticationException
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<String>> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(
                        null,
                        ex.getMessage(),
                        null,
                        HttpStatus.UNAUTHORIZED.value()
                ));
    }

    /**
     * Xử lý UserAlreadyExistsException
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<String>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.error("User already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(
                        null,
                        ex.getMessage(),
                        null,
                        HttpStatus.CONFLICT.value()
                ));
    }

    /**
     * Xử lý MethodArgumentNotValidException (Validation error)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        null,
                        "Validation error",
                        errors,
                        HttpStatus.BAD_REQUEST.value()
                ));
    }

    /**
     * Xử lý general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                        null,
                        "Internal server error",
                        null,
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));
    }
}
