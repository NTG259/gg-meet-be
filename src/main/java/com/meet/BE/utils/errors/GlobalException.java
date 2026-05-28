package com.meet.BE.utils.errors;

import com.meet.BE.domain.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalException {

    // Xử lý lỗi Business
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ApiResponse<>(
                        null,
                        errorCode.getMessage(),
                        errorCode.name(),
                        errorCode.getStatus().value()
                ));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthException(AuthException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ApiResponse<>(
                        null,
                        errorCode.getMessage(),
                        errorCode.name(),
                        errorCode.getStatus().value()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ApiResponse<>(
                        null,
                        errorCode.getMessage(),
                        errors,
                        errorCode.getStatus().value()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ApiResponse<>(
                        null,
                        ex.getMessage() == null || ex.getMessage().isBlank()
                                ? errorCode.getMessage()
                                : ex.getMessage(),
                        errorCode.name(),
                        errorCode.getStatus().value()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleSystemException(Exception ex) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ApiResponse<>(
                        null,
                        errorCode.getMessage(),
                        errorCode.name(),
                        errorCode.getStatus().value()
                ));
    }
}
