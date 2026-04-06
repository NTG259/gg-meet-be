package com.meet.BE.utils.errors;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    //AUTH
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token không hợp lệ"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Token đã hết hạn"),
    USER_DISABLED(HttpStatus.BAD_REQUEST, "Tài khoản đã bị khóa"),
    USER_LOCKED(HttpStatus.BAD_REQUEST, "Tài khoản đã bị khóa"),

    //ROOM
    ROOM_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Room already exists"),
    ROOM_CANNOT_BE_CREATED(HttpStatus.BAD_REQUEST, "Room can't be created"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Room not exists"),
    // DATA
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    EMAIL_ALREADY_EXIST(HttpStatus.CONFLICT, "Email đã được sử dụng"),

    //VALIDATION
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),

    // SYSTEM
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi không mong muốn"),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Đường dẫn không hợp lệ"),
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "Dữ liệu không tồn tại"),

    ;
    private final HttpStatus status;
    private final String message;
    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}