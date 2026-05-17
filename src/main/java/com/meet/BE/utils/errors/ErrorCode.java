package com.meet.BE.utils.errors;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    //AUTH
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Incorrect email or password"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "User is not authenticated"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token or has expired"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Token has expired"),
    USER_DISABLED(HttpStatus.BAD_REQUEST, "Account is disabled"),
    USER_LOCKED(HttpStatus.BAD_REQUEST, "Account is locked"),

    // ROOM
    ROOM_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "Room already exists"),
    ROOM_CANNOT_BE_CREATED(HttpStatus.BAD_REQUEST, "Room cannot be created"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Meeting room not found"),
    NOT_PERMIT(HttpStatus.BAD_REQUEST, "Only the room owner has permission"),
    USER_NOT_IN_THIS_ROOM(HttpStatus.BAD_REQUEST, "User is not in the current meeting room"),
    MEETING_SCHEDULED_NOT_STARTED(HttpStatus.BAD_REQUEST, "Scheduled meeting has not started yet"),
    USER_BANNED_FROM_MEETING(HttpStatus.FORBIDDEN, "User has been banned from this meeting"),
    ROOM_DELETE_FAILED(HttpStatus.BAD_REQUEST, "Room cannot delete"),
    PARTICIPANT_KICKED_FAILED(HttpStatus.BAD_REQUEST, "Failed to kick participant"),
    PARTICIPANT_KICKED(HttpStatus.OK, "User kicked successfully"),
    // DATA
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_ALREADY_EXIST(HttpStatus.CONFLICT, "Email is already in use"),
    DATA_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "Insufficient data provided"),

    // VALIDATION
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid input data"),

    // SYSTEM
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal server error"),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Endpoint not found"),
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "Data not found"),

    INVALID_STATUS(HttpStatus.BAD_REQUEST, "Invalid status"),
    MEETING_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "Meeting has already ended"),

    // SCREEN SHARE
    SCREEN_SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, "No active screen share track found for participant"),
    SCREEN_SHARE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Screen share operation failed"),

    // MUTE
    MUTE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Mute operation failed");
    private final HttpStatus status;
    private final String message;
    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}