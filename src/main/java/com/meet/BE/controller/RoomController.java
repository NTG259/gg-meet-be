package com.meet.BE.controller;

import com.google.protobuf.Api;
import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.request.CreateRoomRequest;
import com.meet.BE.domain.request.JoinRoomRequest;
import com.meet.BE.domain.response.RoomConnectionResponse;
import com.meet.BE.service.RoomService;
import com.meet.BE.utils.errors.BusinessException;
import com.meet.BE.utils.errors.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<RoomConnectionResponse>> createRoom(@RequestBody CreateRoomRequest request) {
        RoomConnectionResponse response = roomService.createRoomAndToken(request);
        return ResponseEntity.ok().body(
                new ApiResponse<>(
                        response,
                        "Call api create room success",
                        null,
                        HttpStatus.CREATED.value()
                )
        );
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<RoomConnectionResponse>> joinRoom(@RequestBody JoinRoomRequest request) {
        RoomConnectionResponse response = roomService.joinRoom(request);
        return ResponseEntity.ok().body(
                new ApiResponse<>(
                        response,
                        "Call api join room success",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }
}

