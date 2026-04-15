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
    ROOM_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Phòng đã tồn tại"),
    ROOM_CANNOT_BE_CREATED(HttpStatus.BAD_REQUEST, "Lỗi không tạo được phòng"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Phòng họp không tồn tại"),
    NOT_PERMIT(HttpStatus.BAD_REQUEST, "Chỉ chủ phòng có quyền"),
    USER_NOT_IN_THIS_ROOM(HttpStatus.BAD_REQUEST, "Người dùng này không thuộc phòng họp hiện tại"),

    // DATA
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    EMAIL_ALREADY_EXIST(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    DATA_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "Thông tin truyền về chưa đầy đủ"),
    //VALIDATION
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),

    // SYSTEM
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp lỗi không mong muốn"),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Đường dẫn không hợp lệ"),
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "Dữ liệu không tồn tại"),

    INVALID_STATUS(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ"),
    MEETING_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "Phòng họp đã kết thúc");
    private final HttpStatus status;
    private final String message;
    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}