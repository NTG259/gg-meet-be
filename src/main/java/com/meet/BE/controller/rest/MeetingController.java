package com.meet.BE.controller.rest;

import com.meet.BE.domain.ApiResponse;
import com.meet.BE.domain.request.CreateMeetingRequest;
import com.meet.BE.domain.entity.User;
import com.meet.BE.domain.response.MeetingResponseDTO;
import com.meet.BE.service.MeetingService;
import com.meet.BE.service.SecurityService;
import com.meet.BE.service.UserService;
import com.meet.BE.utils.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final UserService userService;
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> createMeeting(
            @RequestBody CreateMeetingRequest request
    ) throws Exception {
        String currentUserEmail = SecurityUtils.getCurrentUserJWT().get();
        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        MeetingResponseDTO response = meetingService.createMeeting(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        response,
                        "Tạo phòng thành công",
                        null,
                        HttpStatus.CREATED.value()
                )
        );
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<MeetingResponseDTO>> joinMeeting(
            @RequestParam(value = "meetingCode") String meetingCode
    ) {
        String currentUserEmail = SecurityUtils.getCurrentUserJWT().get();
        User currentUser = this.userService.handleFindUserByEmail(currentUserEmail);
        MeetingResponseDTO response = meetingService.joinMeeting(currentUser, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        response,
                        "Tham gia phòng thành công",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }

//    @PostMapping("/verify")
//    public ResponseEntity<ApiResponse<MeetingResponseDTO>> verifyMeeting(
//            @RequestParam String roomCode,
//            @RequestParam String liveKitToken) {
//        return ResponseEntity.ok().body(
//                new ApiResponse<>(
//                        this.meetingService.handleFindMeeting(roomCode, liveKitToken),
//                        "Phòng có tồn tại",
//                        null,
//                        HttpStatus.OK.value()
//                )
//        );
//    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> endMeeting(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "meetingCode") String meetingCode
    ) throws Exception {
        meetingService.endMeeting(currentUser, meetingCode);
        return ResponseEntity.ok(
                new ApiResponse<>(
                        null,
                        "Kết thúc phòng thành công",
                        null,
                        HttpStatus.OK.value()
                )
        );
    }
}